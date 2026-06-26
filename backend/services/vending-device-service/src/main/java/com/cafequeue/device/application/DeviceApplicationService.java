package com.cafequeue.device.application;

import com.cafequeue.common.contract.DeviceCommandRequest;
import com.cafequeue.common.core.BusinessException;
import com.cafequeue.common.core.IdGenerator;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeviceApplicationService {
    private final JdbcTemplate jdbcTemplate;

    public DeviceApplicationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        seedDevice("MACHINE_A01", "Library 1F", true, 92);
        seedDevice("MACHINE_B02", "Teaching Building B", true, 89);
    }

    public List<DeviceView> list() {
        return jdbcTemplate.query("""
                SELECT device_id, location, online, water_temperature, cleaning_required, updated_at
                FROM vending_device
                ORDER BY device_id
                """, (rs, rowNum) -> new DeviceView(
                rs.getString("device_id"),
                rs.getString("location"),
                rs.getBoolean("online"),
                rs.getInt("water_temperature"),
                rs.getBoolean("cleaning_required"),
                rs.getTimestamp("updated_at").toInstant()
        ));
    }

    public DeviceView updateStatus(String deviceId, boolean online, int waterTemperature) {
        DeviceView current = get(deviceId);
        int updated = jdbcTemplate.update("""
                UPDATE vending_device
                SET online = ?, water_temperature = ?, updated_at = ?
                WHERE device_id = ?
                """, online, waterTemperature, Timestamp.from(Instant.now()), deviceId);
        if (updated == 0) {
            throw new BusinessException("DEVICE_NOT_FOUND", "device not found: " + deviceId);
        }
        return new DeviceView(deviceId, current.location(), online, waterTemperature, current.cleaningRequired(), Instant.now());
    }

    public DeviceCommandView command(String deviceId, DeviceCommandRequest request) {
        DeviceView device = get(deviceId);
        if (!device.online()) {
            throw new BusinessException("DEVICE_OFFLINE", "device is offline: " + deviceId);
        }
        if (device.cleaningRequired()) {
            throw new BusinessException("DEVICE_CLEANING_REQUIRED", "device requires cleaning: " + deviceId);
        }
        String commandId = IdGenerator.prefixed("cmd");
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO device_command(command_id, device_id, order_id, recipe_code, command_type, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, commandId, deviceId, request.orderId(), request.recipeCode(), request.commandType(), "ACCEPTED", Timestamp.from(now));
        return new DeviceCommandView(commandId, deviceId, request.orderId(), request.recipeCode(), "ACCEPTED", now);
    }

    private DeviceView get(String deviceId) {
        List<DeviceView> devices = jdbcTemplate.query("""
                SELECT device_id, location, online, water_temperature, cleaning_required, updated_at
                FROM vending_device
                WHERE device_id = ?
                """, (rs, rowNum) -> new DeviceView(
                rs.getString("device_id"),
                rs.getString("location"),
                rs.getBoolean("online"),
                rs.getInt("water_temperature"),
                rs.getBoolean("cleaning_required"),
                rs.getTimestamp("updated_at").toInstant()
        ), deviceId);
        if (devices.isEmpty()) {
            throw new BusinessException("DEVICE_NOT_FOUND", "device not found: " + deviceId);
        }
        return devices.getFirst();
    }

    private void seedDevice(String deviceId, String location, boolean online, int waterTemperature) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT IGNORE INTO vending_device(device_id, location, online, water_temperature, cleaning_required, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, deviceId, location, online, waterTemperature, false, Timestamp.from(now), Timestamp.from(now));
    }

    public record DeviceView(String deviceId, String location, boolean online, int waterTemperature, boolean cleaningRequired, Instant updatedAt) {
    }

    public record DeviceCommandView(String commandId, String deviceId, String orderId, String recipeCode, String status, Instant createdAt) {
    }
}
