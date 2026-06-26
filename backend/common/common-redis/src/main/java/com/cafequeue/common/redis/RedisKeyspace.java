package com.cafequeue.common.redis;

public final class RedisKeyspace {
    private RedisKeyspace() {
    }

    public static String inventoryReservation(String machineId) {
        return "cq:inventory:reservation:" + machineId;
    }

    public static String machineQueue(String machineId) {
        return "cq:queue:machine:" + machineId;
    }

    public static String gatewayRateLimit(String routeId) {
        return "cq:gateway:ratelimit:" + routeId;
    }
}
