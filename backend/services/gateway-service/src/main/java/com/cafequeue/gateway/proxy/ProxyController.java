package com.cafequeue.gateway.proxy;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
public class ProxyController {
    private static final List<String> HOP_BY_HOP_HEADERS = List.of(
            HttpHeaders.HOST,
            HttpHeaders.CONTENT_LENGTH,
            HttpHeaders.TRANSFER_ENCODING,
            HttpHeaders.CONNECTION
    );

    private final WebClient webClient;
    private final String orderServiceUrl;
    private final String inventoryServiceUrl;
    private final String queueServiceUrl;
    private final String notificationServiceUrl;
    private final String couponServiceUrl;
    private final String deviceServiceUrl;

    public ProxyController(
            WebClient.Builder builder,
            @Value("${cafequeue.routes.order-service:http://localhost:8081}") String orderServiceUrl,
            @Value("${cafequeue.routes.recipe-inventory-service:http://localhost:8082}") String inventoryServiceUrl,
            @Value("${cafequeue.routes.vending-queue-service:http://localhost:8083}") String queueServiceUrl,
            @Value("${cafequeue.routes.notification-service:http://localhost:8084}") String notificationServiceUrl,
            @Value("${cafequeue.routes.marketing-coupon-service:http://localhost:8085}") String couponServiceUrl,
            @Value("${cafequeue.routes.vending-device-service:http://localhost:8086}") String deviceServiceUrl
    ) {
        this.webClient = builder.build();
        this.orderServiceUrl = stripTrailingSlash(orderServiceUrl);
        this.inventoryServiceUrl = stripTrailingSlash(inventoryServiceUrl);
        this.queueServiceUrl = stripTrailingSlash(queueServiceUrl);
        this.notificationServiceUrl = stripTrailingSlash(notificationServiceUrl);
        this.couponServiceUrl = stripTrailingSlash(couponServiceUrl);
        this.deviceServiceUrl = stripTrailingSlash(deviceServiceUrl);
    }

    @RequestMapping("/api/**")
    public Mono<ResponseEntity<byte[]>> proxy(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = targetUri(request);
        return webClient.method(request.getMethod())
                .uri(uri)
                .headers(headers -> request.getHeaders().forEach((name, values) -> {
                    if (!HOP_BY_HOP_HEADERS.contains(name)) {
                        headers.addAll(name, values);
                    }
                }))
                .body(BodyInserters.fromDataBuffers(request.getBody()))
                .exchangeToMono(response -> response.bodyToMono(byte[].class)
                        .defaultIfEmpty(new byte[0])
                        .map(body -> {
                            HttpHeaders headers = new HttpHeaders();
                            response.headers().asHttpHeaders().forEach((name, values) -> {
                                if (!HOP_BY_HOP_HEADERS.contains(name)) {
                                    headers.addAll(name, values);
                                }
                            });
                            return ResponseEntity.status(response.statusCode()).headers(headers).body(body);
                        }));
    }

    private URI targetUri(ServerHttpRequest request) {
        String path = request.getPath().value();
        String baseUrl = baseUrl(path);
        String query = request.getURI().getRawQuery();
        return URI.create(baseUrl + path + (query == null ? "" : "?" + query));
    }

    private String baseUrl(String path) {
        if (path.startsWith("/api/orders")) {
            return orderServiceUrl;
        }
        if (path.startsWith("/api/recipes") || path.startsWith("/api/inventory")) {
            return inventoryServiceUrl;
        }
        if (path.startsWith("/api/queues")) {
            return queueServiceUrl;
        }
        if (path.startsWith("/api/notifications")) {
            return notificationServiceUrl;
        }
        if (path.startsWith("/api/coupons")) {
            return couponServiceUrl;
        }
        if (path.startsWith("/api/devices")) {
            return deviceServiceUrl;
        }
        throw new ResponseStatusException(NOT_FOUND, "route not found");
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
