package com.cafequeue.order.infrastructure.client;

import com.cafequeue.common.contract.DeviceCommandRequest;
import com.cafequeue.common.core.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DeviceClient {
    private final RestClient restClient;

    public DeviceClient(RestClient.Builder builder,
                        @Value("${cafequeue.clients.vending-device-service:http://localhost:8086}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ApiResponse<DeviceCommandView> command(String deviceId, DeviceCommandRequest request) {
        return restClient.post()
                .uri("/api/devices/{deviceId}/commands", deviceId)
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public record DeviceCommandView(String commandId, String deviceId, String orderId, String recipeCode, String status, String createdAt) {
    }
}
