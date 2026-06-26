package com.cafequeue.order.infrastructure.client;

import com.cafequeue.common.contract.CouponLockRequest;
import com.cafequeue.common.contract.CouponLockResponse;
import com.cafequeue.common.core.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CouponClient {
    private final RestClient restClient;

    public CouponClient(RestClient.Builder builder,
                        @Value("${cafequeue.clients.marketing-coupon-service:http://localhost:8085}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ApiResponse<CouponLockResponse> lock(CouponLockRequest request) {
        return restClient.post()
                .uri("/api/coupons/lock")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ApiResponse<Object> redeem(String couponCode) {
        return restClient.post()
                .uri("/api/coupons/{couponCode}/redeem", couponCode)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ApiResponse<Object> release(String couponCode) {
        return restClient.post()
                .uri("/api/coupons/{couponCode}/release", couponCode)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
