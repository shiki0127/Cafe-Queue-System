package com.cafequeue.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "student_id", nullable = false, length = 64)
    private String studentId;

    @Column(name = "machine_id", nullable = false, length = 64)
    private String machineId;

    @Column(name = "recipe_code", nullable = false, length = 64)
    private String recipeCode;

    @Column(name = "coupon_code", length = 64)
    private String couponCode;

    @Column(name = "coupon_lock_id", length = 64)
    private String couponLockId;

    @Column(name = "reservation_id", length = 64)
    private String reservationId;

    @Column(name = "queue_ticket_id", length = 64)
    private String queueTicketId;

    @Column(name = "device_command_id", length = 64)
    private String deviceCommandId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Column(name = "estimated_wait_seconds", nullable = false)
    private long estimatedWaitSeconds;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {
    }

    public OrderEntity(String orderId, String studentId, String machineId, String recipeCode, String couponCode, Instant now) {
        this.orderId = orderId;
        this.studentId = studentId;
        this.machineId = machineId;
        this.recipeCode = recipeCode;
        this.couponCode = couponCode;
        this.status = OrderStatus.CREATED;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getCouponLockId() {
        return couponLockId;
    }

    public void setCouponLockId(String couponLockId) {
        this.couponLockId = couponLockId;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public String getQueueTicketId() {
        return queueTicketId;
    }

    public void setQueueTicketId(String queueTicketId) {
        this.queueTicketId = queueTicketId;
    }

    public String getDeviceCommandId() {
        return deviceCommandId;
    }

    public void setDeviceCommandId(String deviceCommandId) {
        this.deviceCommandId = deviceCommandId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public long getEstimatedWaitSeconds() {
        return estimatedWaitSeconds;
    }

    public void setEstimatedWaitSeconds(long estimatedWaitSeconds) {
        this.estimatedWaitSeconds = estimatedWaitSeconds;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
