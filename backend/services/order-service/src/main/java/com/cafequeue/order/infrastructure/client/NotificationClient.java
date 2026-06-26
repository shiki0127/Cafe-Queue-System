package com.cafequeue.order.infrastructure.client;

import com.cafequeue.common.contract.NotificationRequest;
import com.cafequeue.common.core.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NotificationClient {
    private final RestClient restClient;

    public NotificationClient(RestClient.Builder builder,
                              @Value("${cafequeue.clients.notification-service:http://localhost:8084}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ApiResponse<NotificationView> send(NotificationRequest request) {
        return restClient.post()
                .uri("/api/notifications")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public record NotificationView(String notificationId, String studentId, String title, String content, String businessId, String sentAt) {
    }
}
