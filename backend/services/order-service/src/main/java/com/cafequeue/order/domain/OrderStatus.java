package com.cafequeue.order.domain;

public enum OrderStatus {
    CREATED,
    QUEUED,
    BREWING,
    COMPLETED,
    FAILED,
    EXCEPTION
}
