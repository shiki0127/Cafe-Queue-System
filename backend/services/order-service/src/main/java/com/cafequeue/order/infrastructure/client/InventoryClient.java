package com.cafequeue.order.infrastructure.client;

import com.cafequeue.common.contract.InventoryReservationRequest;
import com.cafequeue.common.contract.InventoryReservationResponse;
import com.cafequeue.common.core.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventoryClient {
    private final RestClient restClient;

    public InventoryClient(RestClient.Builder builder,
                           @Value("${cafequeue.clients.recipe-inventory-service:http://localhost:8082}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ApiResponse<InventoryReservationResponse> reserve(InventoryReservationRequest request) {
        return restClient.post()
                .uri("/api/inventory/reservations")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ApiResponse<InventoryReservationResponse> commit(String reservationId) {
        return restClient.post()
                .uri("/api/inventory/reservations/{reservationId}/commit", reservationId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ApiResponse<InventoryReservationResponse> release(String reservationId) {
        return restClient.post()
                .uri("/api/inventory/reservations/{reservationId}/release", reservationId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
