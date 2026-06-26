package com.cafequeue.order.infrastructure.client;

import com.cafequeue.common.contract.QueueDispatchRequest;
import com.cafequeue.common.contract.QueueDispatchResponse;
import com.cafequeue.common.core.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class QueueClient {
    private final RestClient restClient;

    public QueueClient(RestClient.Builder builder,
                       @Value("${cafequeue.clients.vending-queue-service:http://localhost:8083}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public ApiResponse<QueueDispatchResponse> dispatch(QueueDispatchRequest request) {
        return restClient.post()
                .uri("/api/queues/dispatch")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public ApiResponse<QueueTicketView> complete(String ticketId) {
        return restClient.post()
                .uri("/api/queues/{ticketId}/complete", ticketId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    public record QueueTicketView(
            String ticketId,
            String orderId,
            String machineId,
            String recipeCode,
            int position,
            long estimatedWaitSeconds,
            String status,
            String createdAt
    ) {
    }
}
