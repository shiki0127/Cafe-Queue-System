package com.cafequeue.common.contract;

public record InventoryReservationRequest(String orderId, String machineId, String recipeCode, int quantity) {
}
