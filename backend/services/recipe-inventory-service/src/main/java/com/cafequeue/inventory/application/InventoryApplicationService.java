package com.cafequeue.inventory.application;

import com.cafequeue.common.contract.InventoryReservationRequest;
import com.cafequeue.common.contract.InventoryReservationResponse;
import com.cafequeue.common.core.BusinessException;
import com.cafequeue.common.core.IdGenerator;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class InventoryApplicationService {
    private static final String RESERVE_SCRIPT = """
            if redis.call('HEXISTS', KEYS[2], ARGV[1]) == 1 then
              return 1
            end
            for i = 3, #ARGV, 2 do
              local current = tonumber(redis.call('HGET', KEYS[1], ARGV[i]) or '0')
              local required = tonumber(ARGV[i + 1])
              if current < required then
                return 0
              end
            end
            for i = 3, #ARGV, 2 do
              redis.call('HINCRBY', KEYS[1], ARGV[i], -tonumber(ARGV[i + 1]))
            end
            redis.call('HSET', KEYS[2], ARGV[1], ARGV[2])
            redis.call('EXPIRE', KEYS[2], 86400)
            return 1
            """;

    private static final String STOCK_PREFIX = "cq:inventory:stock:";
    private static final String RESERVATION_KEY = "cq:inventory:reservations";
    private static final Map<String, Integer> DEFAULT_STOCK = Map.of(
            "coffee_mg", 1800000,
            "milk_mg", 3600000,
            "water_mg", 8000000
    );

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public InventoryApplicationService(StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        seedRecipes();
    }

    public List<RecipeView> recipes() {
        List<RecipeView> recipes = jdbcTemplate.query("""
                SELECT recipe_code, recipe_name
                FROM recipe
                WHERE enabled = 1
                ORDER BY recipe_code
                """, (rs, rowNum) -> new RecipeView(
                rs.getString("recipe_code"),
                rs.getString("recipe_name"),
                recipeIngredientsFromDb(rs.getString("recipe_code"))
        ));
        return recipes.isEmpty() ? RECIPE_VIEWS : recipes;
    }

    private static final List<RecipeView> RECIPE_VIEWS = List.of(
                new RecipeView("LATTE", "Campus Latte", Map.of("coffee_mg", 18000, "milk_mg", 120000)),
                new RecipeView("AMERICANO", "Library Americano", Map.of("coffee_mg", 18000, "water_mg", 180000))
        );

    public Map<String, Integer> machineInventory(String machineId) {
        ensureMachineStock(machineId);
        Map<Object, Object> values = redisTemplate.opsForHash().entries(stockKey(machineId));
        Map<String, Integer> result = new HashMap<>();
        values.forEach((key, value) -> result.put(String.valueOf(key), Integer.parseInt(String.valueOf(value))));
        syncMachineInventory(machineId, result);
        return result;
    }

    public InventoryReservationResponse reserve(InventoryReservationRequest request) {
        ensureMachineStock(request.machineId());
        Map<String, Integer> ingredients = recipeIngredients(request.recipeCode(), request.quantity());
        String reservationId = IdGenerator.prefixed("res");
        String payload = reservationPayload("RESERVED", request.machineId(), request.recipeCode(), ingredients);
        String[] args = reserveArgs(reservationId, payload, ingredients);
        Long reserved = redisTemplate.execute(
                new DefaultRedisScript<>(RESERVE_SCRIPT, Long.class),
                List.of(stockKey(request.machineId()), RESERVATION_KEY),
                (Object[]) args
        );
        if (!Long.valueOf(1).equals(reserved)) {
            return new InventoryReservationResponse(reservationId, false, "inventory not enough");
        }
        upsertReservation(reservationId, request.orderId(), request.machineId(), request.recipeCode(), "RESERVED");
        syncMachineInventory(request.machineId(), machineInventory(request.machineId()));
        InventoryReservationResponse response = new InventoryReservationResponse(reservationId, true, "reserved");
        return response;
    }

    public InventoryReservationResponse commit(String reservationId) {
        ReservationPayload payload = loadReservation(reservationId);
        if (!"RESERVED".equals(payload.status()) && !"COMMITTED".equals(payload.status())) {
            throw new BusinessException("INVENTORY_RESERVATION_NOT_RESERVED", "reservation is not reserved: " + reservationId);
        }
        redisTemplate.opsForHash().put(RESERVATION_KEY, reservationId, reservationPayload("COMMITTED", payload.machineId(), payload.recipeCode(), payload.ingredients()));
        updateReservationStatus(reservationId, "COMMITTED");
        return new InventoryReservationResponse(reservationId, true, "committed");
    }

    public InventoryReservationResponse release(String reservationId) {
        ReservationPayload payload = loadReservation(reservationId);
        if ("RESERVED".equals(payload.status())) {
            payload.ingredients().forEach((name, amount) -> redisTemplate.opsForHash().increment(stockKey(payload.machineId()), name, amount));
            redisTemplate.opsForHash().put(RESERVATION_KEY, reservationId, reservationPayload("RELEASED", payload.machineId(), payload.recipeCode(), payload.ingredients()));
            updateReservationStatus(reservationId, "RELEASED");
            syncMachineInventory(payload.machineId(), machineInventory(payload.machineId()));
        }
        return new InventoryReservationResponse(reservationId, true, "released");
    }

    private void ensureMachineStock(String machineId) {
        String key = stockKey(machineId);
        DEFAULT_STOCK.forEach((name, amount) -> redisTemplate.opsForHash().putIfAbsent(key, name, amount.toString()));
        redisTemplate.expire(key, Duration.ofDays(7));
    }

    private Map<String, Integer> recipeIngredients(String recipeCode, int quantity) {
        int safeQuantity = Math.max(quantity, 1);
        RecipeView recipe = recipes().stream()
                .filter(item -> item.recipeCode().equalsIgnoreCase(recipeCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException("RECIPE_NOT_FOUND", "recipe not found: " + recipeCode));
        return recipe.ingredients().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() * safeQuantity));
    }

    private String[] reserveArgs(String reservationId, String payload, Map<String, Integer> ingredients) {
        String[] args = new String[2 + ingredients.size() * 2];
        args[0] = reservationId;
        args[1] = payload;
        int index = 2;
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            args[index++] = entry.getKey();
            args[index++] = entry.getValue().toString();
        }
        return args;
    }

    private ReservationPayload loadReservation(String reservationId) {
        Object raw = redisTemplate.opsForHash().get(RESERVATION_KEY, reservationId);
        if (raw == null) {
            throw new BusinessException("INVENTORY_RESERVATION_NOT_FOUND", "reservation not found: " + reservationId);
        }
        return parseReservation(String.valueOf(raw));
    }

    private ReservationPayload parseReservation(String raw) {
        String[] parts = raw.split("\\|", 4);
        Map<String, Integer> ingredients = Arrays.stream(parts[3].split(","))
                .map(item -> item.split("=", 2))
                .collect(Collectors.toMap(item -> item[0], item -> Integer.parseInt(item[1])));
        return new ReservationPayload(parts[0], parts[1], parts[2], ingredients);
    }

    private String reservationPayload(String status, String machineId, String recipeCode, Map<String, Integer> ingredients) {
        String ingredientText = ingredients.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        return status + "|" + machineId + "|" + recipeCode + "|" + ingredientText;
    }

    private String stockKey(String machineId) {
        return STOCK_PREFIX + machineId;
    }

    private Map<String, Integer> recipeIngredientsFromDb(String recipeCode) {
        return jdbcTemplate.query("""
                SELECT ingredient_code, amount_mg
                FROM recipe_ingredient
                WHERE recipe_code = ?
                """, (rs, rowNum) -> Map.entry(rs.getString("ingredient_code"), rs.getInt("amount_mg")), recipeCode)
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void seedRecipes() {
        Instant now = Instant.now();
        for (RecipeView recipe : RECIPE_VIEWS) {
            jdbcTemplate.update("""
                    INSERT IGNORE INTO recipe(recipe_code, recipe_name, enabled, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, recipe.recipeCode(), recipe.name(), true, Timestamp.from(now), Timestamp.from(now));
            recipe.ingredients().forEach((ingredient, amount) -> jdbcTemplate.update("""
                    INSERT IGNORE INTO recipe_ingredient(recipe_code, ingredient_code, amount_mg)
                    VALUES (?, ?, ?)
                    """, recipe.recipeCode(), ingredient, amount));
        }
    }

    private void syncMachineInventory(String machineId, Map<String, Integer> stock) {
        stock.forEach((ingredient, amount) -> jdbcTemplate.update("""
                INSERT INTO machine_inventory(machine_id, ingredient_code, available_mg, locked_mg)
                VALUES (?, ?, ?, 0)
                ON DUPLICATE KEY UPDATE available_mg = VALUES(available_mg)
                """, machineId, ingredient, amount));
    }

    private void upsertReservation(String reservationId, String orderId, String machineId, String recipeCode, String status) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO inventory_reservation(reservation_id, order_id, machine_id, recipe_code, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = VALUES(updated_at)
                """, reservationId, orderId, machineId, recipeCode, status, Timestamp.from(now), Timestamp.from(now));
    }

    private void updateReservationStatus(String reservationId, String status) {
        jdbcTemplate.update("""
                UPDATE inventory_reservation
                SET status = ?, updated_at = ?
                WHERE reservation_id = ?
                """, status, Timestamp.from(Instant.now()), reservationId);
    }

    public record RecipeView(String recipeCode, String name, Map<String, Integer> ingredients) {
    }

    private record ReservationPayload(String status, String machineId, String recipeCode, Map<String, Integer> ingredients) {
    }
}
