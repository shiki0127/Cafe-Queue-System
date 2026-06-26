package com.cafequeue.gateway.filter;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class RedisRateLimitFilter implements WebFilter, Ordered {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final long limitPerMinute;

    public RedisRateLimitFilter(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${cafequeue.gateway.rate-limit-per-minute:120}") long limitPerMinute
    ) {
        this.redisTemplate = redisTemplate;
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String key = rateLimitKey(exchange);
        return redisTemplate.opsForValue().increment(key)
                .flatMap(count -> {
                    Mono<Boolean> expire = count == 1 ? redisTemplate.expire(key, Duration.ofMinutes(1)) : Mono.just(true);
                    if (count > limitPerMinute) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return expire.then(exchange.getResponse().setComplete());
                    }
                    return expire.then(chain.filter(exchange));
                });
    }

    @Override
    public int getOrder() {
        return -90;
    }

    private String rateLimitKey(ServerWebExchange exchange) {
        String routeId = routeId(exchange.getRequest().getPath().value());
        String principal = exchange.getRequest().getHeaders().getFirst("X-Student-Id");
        if (principal == null || principal.isBlank()) {
            principal = exchange.getRequest().getRemoteAddress() == null
                    ? "anonymous"
                    : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "cq:gateway:ratelimit:" + routeId + ":" + principal;
    }

    private String routeId(String path) {
        if (path.startsWith("/api/orders")) {
            return "order-service";
        }
        if (path.startsWith("/api/recipes") || path.startsWith("/api/inventory")) {
            return "recipe-inventory-service";
        }
        if (path.startsWith("/api/queues")) {
            return "vending-queue-service";
        }
        if (path.startsWith("/api/notifications")) {
            return "notification-service";
        }
        if (path.startsWith("/api/coupons")) {
            return "marketing-coupon-service";
        }
        if (path.startsWith("/api/devices")) {
            return "vending-device-service";
        }
        return "unknown";
    }
}
