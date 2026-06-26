package com.cafequeue.common.contract;

public record QueueDispatchRequest(String orderId, String machineId, String recipeCode) {
}
