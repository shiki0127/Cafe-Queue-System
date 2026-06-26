package com.cafequeue.queue.application;

import com.cafequeue.common.contract.QueueDispatchRequest;
import com.cafequeue.common.contract.QueueDispatchResponse;
import com.cafequeue.common.core.BusinessException;
import com.cafequeue.common.core.IdGenerator;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class QueueApplicationService {
    private static final int DEFAULT_BREW_SECONDS = 90;
    private static final int ID_GENERATION_ATTEMPTS = 20;
    private static final String ENQUEUE_SCRIPT = """
            redis.call('RPUSH', KEYS[1], ARGV[1])
            return redis.call('LLEN', KEYS[1])
            """;
    private static final String MACHINE_QUEUE_PREFIX = "cq:queue:machine:";
    private static final String TICKET_HASH = "cq:queue:tickets";

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public QueueApplicationService(StringRedisTemplate redisTemplate, JdbcTemplate jdbcTemplate) {
        this.redisTemplate = redisTemplate;
        this.jdbcTemplate = jdbcTemplate;
        seedDispatchRule();
    }

    public QueueDispatchResponse dispatch(QueueDispatchRequest request) {
        String ticketId = nextTicketId();
        Long position = redisTemplate.execute(
                new DefaultRedisScript<>(ENQUEUE_SCRIPT, Long.class),
                List.of(machineQueueKey(request.machineId())),
                ticketId
        );
        int queuePosition = Math.toIntExact(position == null ? 1 : position);
        long waitSeconds = (long) queuePosition * DEFAULT_BREW_SECONDS;
        QueueTicketView ticket = new QueueTicketView(ticketId, request.orderId(), request.machineId(), request.recipeCode(), queuePosition, waitSeconds, "WAITING", Instant.now());
        redisTemplate.opsForHash().put(TICKET_HASH, ticketId, serialize(ticket));
        saveTicket(ticket);
        return new QueueDispatchResponse(ticketId, queuePosition, waitSeconds);
    }

    private String nextTicketId() {
        for (int attempt = 0; attempt < ID_GENERATION_ATTEMPTS; attempt++) {
            String candidate = IdGenerator.prefixed("que");
            if (!redisTemplate.opsForHash().hasKey(TICKET_HASH, candidate) && ticketMissingInDatabase(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("QUEUE_TICKET_ID_EXHAUSTED", "unable to allocate a readable queue ticket id");
    }

    public List<QueueTicketView> machineQueue(String machineId) {
        List<String> ticketIds = redisTemplate.opsForList().range(machineQueueKey(machineId), 0, -1);
        if (ticketIds == null || ticketIds.isEmpty()) {
            return List.of();
        }
        return ticketIds.stream()
                .map(ticketId -> redisTemplate.opsForHash().get(TICKET_HASH, ticketId))
                .filter(Objects::nonNull)
                .map(raw -> parse(String.valueOf(raw)))
                .toList();
    }

    public QueueTicketView complete(String ticketId) {
        Object raw = redisTemplate.opsForHash().get(TICKET_HASH, ticketId);
        if (raw == null) {
            throw new BusinessException("QUEUE_TICKET_NOT_FOUND", "queue ticket not found: " + ticketId);
        }
        QueueTicketView ticket = parse(String.valueOf(raw));
        redisTemplate.opsForList().remove(machineQueueKey(ticket.machineId()), 1, ticketId);
        QueueTicketView completed = new QueueTicketView(ticket.ticketId(), ticket.orderId(), ticket.machineId(), ticket.recipeCode(), ticket.position(), 0, "COMPLETED", ticket.createdAt());
        redisTemplate.opsForHash().put(TICKET_HASH, ticketId, serialize(completed));
        updateTicketStatus(ticketId, "COMPLETED", 0);
        return completed;
    }

    private String serialize(QueueTicketView ticket) {
        return String.join("|",
                ticket.ticketId(),
                ticket.orderId(),
                ticket.machineId(),
                ticket.recipeCode(),
                Integer.toString(ticket.position()),
                Long.toString(ticket.estimatedWaitSeconds()),
                ticket.status(),
                ticket.createdAt().toString()
        );
    }

    private QueueTicketView parse(String raw) {
        String[] parts = raw.split("\\|", 8);
        if (parts.length != 8) {
            throw new BusinessException("QUEUE_TICKET_CORRUPTED", "queue ticket data is corrupted");
        }
        return new QueueTicketView(
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                Integer.parseInt(parts[4]),
                Long.parseLong(parts[5]),
                parts[6],
                Instant.parse(parts[7])
        );
    }

    private String machineQueueKey(String machineId) {
        return MACHINE_QUEUE_PREFIX + machineId;
    }

    private void saveTicket(QueueTicketView ticket) {
        Timestamp createdAt = Timestamp.from(ticket.createdAt());
        jdbcTemplate.update("""
                INSERT INTO queue_ticket(ticket_id, order_id, machine_id, recipe_code, queue_position, estimated_wait_seconds, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE queue_position = VALUES(queue_position), estimated_wait_seconds = VALUES(estimated_wait_seconds), status = VALUES(status), updated_at = VALUES(updated_at)
                """, ticket.ticketId(), ticket.orderId(), ticket.machineId(), ticket.recipeCode(), ticket.position(), ticket.estimatedWaitSeconds(), ticket.status(), createdAt, createdAt);
    }

    private void updateTicketStatus(String ticketId, String status, long estimatedWaitSeconds) {
        jdbcTemplate.update("""
                UPDATE queue_ticket
                SET status = ?, estimated_wait_seconds = ?, updated_at = ?
                WHERE ticket_id = ?
                """, status, estimatedWaitSeconds, Timestamp.from(Instant.now()), ticketId);
    }

    private boolean ticketMissingInDatabase(String ticketId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue_ticket WHERE ticket_id = ?",
                Integer.class,
                ticketId
        );
        return count == null || count == 0;
    }

    private void seedDispatchRule() {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT IGNORE INTO dispatch_rule(rule_code, rule_name, enabled, weight, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, "FIFO_PER_MACHINE", "Machine local FIFO queue", true, 100, Timestamp.from(now), Timestamp.from(now));
    }

    public record QueueTicketView(
            String ticketId,
            String orderId,
            String machineId,
            String recipeCode,
            int position,
            long estimatedWaitSeconds,
            String status,
            Instant createdAt
    ) {
    }
}
