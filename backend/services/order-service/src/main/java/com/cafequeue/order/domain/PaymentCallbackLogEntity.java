package com.cafequeue.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_callback_log")
public class PaymentCallbackLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "callback_id", nullable = false, unique = true, length = 128)
    private String callbackId;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "handled", nullable = false)
    private boolean handled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentCallbackLogEntity() {
    }

    public PaymentCallbackLogEntity(String callbackId, String orderId, String payload, Instant createdAt) {
        this.callbackId = callbackId;
        this.orderId = orderId;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public boolean isHandled() {
        return handled;
    }

    public String getOrderId() {
        return orderId;
    }

    public void markHandled() {
        this.handled = true;
    }
}
