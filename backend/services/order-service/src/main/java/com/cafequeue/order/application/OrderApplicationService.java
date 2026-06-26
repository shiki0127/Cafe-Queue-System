package com.cafequeue.order.application;

import com.cafequeue.common.contract.CouponLockRequest;
import com.cafequeue.common.contract.CouponLockResponse;
import com.cafequeue.common.contract.DeviceCommandRequest;
import com.cafequeue.common.contract.InventoryReservationRequest;
import com.cafequeue.common.contract.InventoryReservationResponse;
import com.cafequeue.common.contract.NotificationRequest;
import com.cafequeue.common.contract.QueueDispatchRequest;
import com.cafequeue.common.contract.QueueDispatchResponse;
import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.common.core.BusinessException;
import com.cafequeue.common.core.IdGenerator;
import com.cafequeue.order.domain.OrderEntity;
import com.cafequeue.order.domain.OrderStateHistoryEntity;
import com.cafequeue.order.domain.OrderStatus;
import com.cafequeue.order.domain.PaymentCallbackLogEntity;
import com.cafequeue.order.infrastructure.client.CouponClient;
import com.cafequeue.order.infrastructure.client.DeviceClient;
import com.cafequeue.order.infrastructure.client.InventoryClient;
import com.cafequeue.order.infrastructure.client.NotificationClient;
import com.cafequeue.order.infrastructure.client.QueueClient;
import com.cafequeue.order.infrastructure.persistence.OrderRepository;
import com.cafequeue.order.infrastructure.persistence.OrderStateHistoryRepository;
import com.cafequeue.order.infrastructure.persistence.PaymentCallbackLogRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService {
    private static final BigDecimal DEFAULT_ORDER_AMOUNT = BigDecimal.valueOf(18);
    private static final int ID_GENERATION_ATTEMPTS = 20;

    private final OrderRepository orderRepository;
    private final OrderStateHistoryRepository historyRepository;
    private final PaymentCallbackLogRepository callbackLogRepository;
    private final CouponClient couponClient;
    private final InventoryClient inventoryClient;
    private final QueueClient queueClient;
    private final DeviceClient deviceClient;
    private final NotificationClient notificationClient;

    public OrderApplicationService(
            OrderRepository orderRepository,
            OrderStateHistoryRepository historyRepository,
            PaymentCallbackLogRepository callbackLogRepository,
            CouponClient couponClient,
            InventoryClient inventoryClient,
            QueueClient queueClient,
            DeviceClient deviceClient,
            NotificationClient notificationClient
    ) {
        this.orderRepository = orderRepository;
        this.historyRepository = historyRepository;
        this.callbackLogRepository = callbackLogRepository;
        this.couponClient = couponClient;
        this.inventoryClient = inventoryClient;
        this.queueClient = queueClient;
        this.deviceClient = deviceClient;
        this.notificationClient = notificationClient;
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public OrderView create(String studentId, String machineId, String recipeCode, String couponCode) {
        Instant now = Instant.now();
        String orderId = nextOrderId();
        OrderEntity order = orderRepository.save(new OrderEntity(orderId, studentId, machineId, recipeCode, couponCode, now));
        historyRepository.save(new OrderStateHistoryEntity(orderId, null, OrderStatus.CREATED, "order accepted", now));

        try {
            CouponLockResponse coupon = lockCoupon(studentId, couponCode, orderId);
            if (coupon != null) {
                order.setCouponLockId(coupon.couponLockId());
            }

            InventoryReservationResponse reservation = reserveInventory(orderId, machineId, recipeCode);
            order.setReservationId(reservation.reservationId());

            QueueDispatchResponse queue = dispatchQueue(orderId, machineId, recipeCode);
            order.setQueueTicketId(queue.queueTicketId());
            order.setEstimatedWaitSeconds(queue.estimatedWaitSeconds());
            transition(order, OrderStatus.QUEUED, "inventory reserved and queue ticket created");
            return toView(orderRepository.save(order));
        } catch (RuntimeException ex) {
            releaseReservationQuietly(order.getReservationId());
            releaseCouponQuietly(order.getCouponCode());
            failOrder(order, "CREATE_FLOW_FAILED: " + ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public OrderView get(String orderId) {
        return toView(findOrder(orderId));
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public OrderView markPaid(String orderId, String callbackId, String payload) {
        PaymentCallbackLogEntity callbackLog = recordCallback(orderId, callbackId, payload);
        OrderEntity current = findOrder(orderId);
        if (callbackLog.isHandled() || OrderStatus.BREWING.equals(current.getStatus())) {
            if (!callbackLog.isHandled()) {
                callbackLog.markHandled();
                callbackLogRepository.save(callbackLog);
            }
            return toView(current);
        }

        try {
            if (current.getReservationId() != null) {
                requireSuccess(inventoryClient.commit(current.getReservationId()), "INVENTORY_COMMIT_FAILED").data();
            }
            if (current.getCouponCode() != null && !current.getCouponCode().isBlank()) {
                requireSuccess(couponClient.redeem(current.getCouponCode()), "COUPON_REDEEM_FAILED");
            }

            DeviceClient.DeviceCommandView command = requireSuccess(
                    deviceClient.command(
                            current.getMachineId(),
                            new DeviceCommandRequest(current.getMachineId(), current.getOrderId(), current.getRecipeCode(), "BREW")
                    ),
                    "DEVICE_COMMAND_FAILED"
            ).data();

            notificationClient.send(new NotificationRequest(
                    current.getStudentId(),
                    "咖啡正在制作中",
                    "你的饮品已开始制作，请留意取餐提醒。订单号：" + current.getOrderId() + "，取餐码：" + current.getQueueTicketId(),
                    current.getOrderId()
            ));

            current.setDeviceCommandId(command.commandId());
            transition(current, OrderStatus.BREWING, "payment callback confirmed");
            callbackLog.markHandled();
            callbackLogRepository.save(callbackLog);
            return toView(orderRepository.save(current));
        } catch (RuntimeException ex) {
            transition(current, OrderStatus.EXCEPTION, "PAYMENT_FLOW_FAILED: " + ex.getMessage());
            current.setFailureReason(trimReason(ex.getMessage()));
            sendBestEffortNotification(
                    current,
                    "订单需要人工处理",
                    "已收到支付信息，但制作流程需要人工确认。订单号：" + current.getOrderId()
            );
            orderRepository.save(current);
            throw ex;
        }
    }

    @Transactional(noRollbackFor = RuntimeException.class)
    public OrderView complete(String orderId) {
        OrderEntity current = findOrder(orderId);
        if (OrderStatus.COMPLETED.equals(current.getStatus())) {
            return toView(current);
        }
        if (current.getQueueTicketId() == null || current.getQueueTicketId().isBlank()) {
            throw new BusinessException("QUEUE_TICKET_REQUIRED", "order has no queue ticket: " + orderId);
        }

        requireSuccess(queueClient.complete(current.getQueueTicketId()), "QUEUE_COMPLETE_FAILED");
        transition(current, OrderStatus.COMPLETED, "demo completion confirmed");
        OrderEntity completed = orderRepository.save(current);
        sendBestEffortNotification(
                completed,
                "饮品已完成",
                "你的饮品已经制作完成，请凭取餐码到咖啡机取餐。取餐码：" + completed.getQueueTicketId()
        );
        return toView(completed);
    }

    private String nextOrderId() {
        for (int attempt = 0; attempt < ID_GENERATION_ATTEMPTS; attempt++) {
            String candidate = IdGenerator.prefixed("ord");
            if (!orderRepository.existsByOrderId(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("ORDER_ID_EXHAUSTED", "unable to allocate a readable order id");
    }

    private PaymentCallbackLogEntity recordCallback(String orderId, String callbackId, String payload) {
        if (callbackId == null || callbackId.isBlank()) {
            throw new BusinessException("PAYMENT_CALLBACK_ID_REQUIRED", "payment callback id is required");
        }
        return callbackLogRepository.findByCallbackId(callbackId).map(existing -> {
            if (!existing.getOrderId().equals(orderId)) {
                throw new BusinessException("PAYMENT_CALLBACK_ORDER_MISMATCH", "payment callback id belongs to another order");
            }
            return existing;
        }).orElseGet(() -> {
            try {
                return callbackLogRepository.save(new PaymentCallbackLogEntity(callbackId, orderId, payload, Instant.now()));
            } catch (DataIntegrityViolationException ignored) {
                return callbackLogRepository.findByCallbackId(callbackId)
                        .orElseThrow(() -> new BusinessException("PAYMENT_CALLBACK_LOG_FAILED", "payment callback log was not saved"));
            }
        });
    }

    private OrderEntity findOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND", "order not found: " + orderId));
    }

    private void transition(OrderEntity order, OrderStatus toStatus, String reason) {
        OrderStatus fromStatus = order.getStatus();
        order.setStatus(toStatus);
        order.setFailureReason(OrderStatus.FAILED.equals(toStatus) || OrderStatus.EXCEPTION.equals(toStatus) ? trimReason(reason) : null);
        order.touch(Instant.now());
        historyRepository.save(new OrderStateHistoryEntity(order.getOrderId(), fromStatus, toStatus, trimReason(reason), Instant.now()));
    }

    private void failOrder(OrderEntity order, String reason) {
        transition(order, OrderStatus.FAILED, reason);
        orderRepository.save(order);
    }

    private void releaseReservationQuietly(String reservationId) {
        if (reservationId == null || reservationId.isBlank()) {
            return;
        }
        try {
            inventoryClient.release(reservationId);
        } catch (RuntimeException ignored) {
            // The failed order keeps its reservation id so operators can reconcile later.
        }
    }

    private void releaseCouponQuietly(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return;
        }
        try {
            couponClient.release(couponCode);
        } catch (RuntimeException ignored) {
            // The failed order keeps its coupon code so operators can reconcile later.
        }
    }

    private OrderView toView(OrderEntity order) {
        return new OrderView(
                order.getOrderId(),
                order.getStudentId(),
                order.getMachineId(),
                order.getRecipeCode(),
                order.getCouponCode(),
                order.getCouponLockId(),
                order.getReservationId(),
                order.getQueueTicketId(),
                order.getDeviceCommandId(),
                order.getStatus().name(),
                order.getEstimatedWaitSeconds(),
                order.getFailureReason(),
                order.getCreatedAt()
        );
    }

    private String trimReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > 255 ? reason.substring(0, 255) : reason;
    }

    private void sendBestEffortNotification(OrderEntity order, String title, String message) {
        try {
            notificationClient.send(new NotificationRequest(
                    order.getStudentId(),
                    title,
                    message,
                    order.getOrderId()
            ));
        } catch (RuntimeException ignored) {
            // Notification is useful but must not hide the original payment failure.
        }
    }

    private CouponLockResponse lockCoupon(String studentId, String couponCode, String orderId) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        CouponLockResponse response = requireSuccess(
                couponClient.lock(new CouponLockRequest(studentId, couponCode, DEFAULT_ORDER_AMOUNT, orderId)),
                "COUPON_LOCK_FAILED"
        ).data();
        if (!response.usable()) {
            throw new BusinessException("COUPON_UNUSABLE", "coupon is not usable: " + couponCode);
        }
        return response;
    }

    private InventoryReservationResponse reserveInventory(String orderId, String machineId, String recipeCode) {
        InventoryReservationResponse response = requireSuccess(
                inventoryClient.reserve(new InventoryReservationRequest(orderId, machineId, recipeCode, 1)),
                "INVENTORY_RESERVE_FAILED"
        ).data();
        if (!response.reserved()) {
            throw new BusinessException("INVENTORY_NOT_ENOUGH", response.message());
        }
        return response;
    }

    private QueueDispatchResponse dispatchQueue(String orderId, String machineId, String recipeCode) {
        return requireSuccess(
                queueClient.dispatch(new QueueDispatchRequest(orderId, machineId, recipeCode)),
                "QUEUE_DISPATCH_FAILED"
        ).data();
    }

    private <T> ApiResponse<T> requireSuccess(ApiResponse<T> response, String errorCode) {
        if (response == null || !response.success()) {
            String message = response == null ? "remote service returned no response" : response.message();
            throw new BusinessException(errorCode, message);
        }
        return response;
    }

    public record OrderView(
            String orderId,
            String studentId,
            String machineId,
            String recipeCode,
            String couponCode,
            String couponLockId,
            String reservationId,
            String queueTicketId,
            String deviceCommandId,
            String status,
            long estimatedWaitSeconds,
            String failureReason,
            Instant createdAt
    ) {
    }
}
