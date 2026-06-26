package com.cafequeue.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_state_history")
public class OrderStateHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(name = "from_status", length = 32)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 32)
    private String toStatus;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OrderStateHistoryEntity() {
    }

    public OrderStateHistoryEntity(String orderId, OrderStatus fromStatus, OrderStatus toStatus, String reason, Instant createdAt) {
        this.orderId = orderId;
        this.fromStatus = fromStatus == null ? null : fromStatus.name();
        this.toStatus = toStatus.name();
        this.reason = reason;
        this.createdAt = createdAt;
    }
}
