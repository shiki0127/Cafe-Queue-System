package com.cafequeue.common.contract;

public record InventoryReservationResponse(String reservationId, boolean reserved, String message) {
}
