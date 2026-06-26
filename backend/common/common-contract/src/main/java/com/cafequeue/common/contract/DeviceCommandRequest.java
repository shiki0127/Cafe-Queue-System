package com.cafequeue.common.contract;

public record DeviceCommandRequest(String deviceId, String orderId, String recipeCode, String commandType) {
}
