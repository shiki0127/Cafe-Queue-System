package com.cafequeue.order.interfaces;

import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.order.application.OrderApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderApplicationService orderService;

    public OrderController(OrderApplicationService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<OrderApplicationService.OrderView> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(request.studentId(), request.machineId(), request.recipeCode(), request.couponCode()));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderApplicationService.OrderView> get(@PathVariable String orderId) {
        return ApiResponse.ok(orderService.get(orderId));
    }

    @PostMapping("/{orderId}/payment-callback")
    public ApiResponse<OrderApplicationService.OrderView> paymentCallback(
            @PathVariable String orderId,
            @Valid @RequestBody PaymentCallbackRequest request
    ) {
        return ApiResponse.ok(orderService.markPaid(orderId, request.callbackId(), request.payload()));
    }

    @PostMapping("/{orderId}/complete")
    public ApiResponse<OrderApplicationService.OrderView> complete(@PathVariable String orderId) {
        return ApiResponse.ok(orderService.complete(orderId));
    }

    public record CreateOrderRequest(
            @NotBlank String studentId,
            @NotBlank String machineId,
            @NotBlank String recipeCode,
            String couponCode
    ) {
    }

    public record PaymentCallbackRequest(
            @NotBlank String callbackId,
            String payload
    ) {
    }
}
