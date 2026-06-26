package com.cafequeue.gateway.filter;

import com.cafequeue.common.security.JwtPrincipal;
import com.cafequeue.common.security.JwtTokenService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {
    private final JwtTokenService jwtTokenService;
    private final List<String> publicPaths = List.of("/actuator", "/api/auth/token");

    public JwtAuthenticationFilter(@Value("${cafequeue.security.jwt-secret}") String secret) {
        this.jwtTokenService = new JwtTokenService(secret);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS || publicPaths.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(exchange, "missing bearer token");
        }

        try {
            JwtPrincipal principal = jwtTokenService.verify(authorization.substring(7));
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .header("X-Student-Id", principal.subject())
                    .header("X-Role", principal.role())
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (IllegalArgumentException ex) {
            return unauthorized(exchange, "invalid bearer token");
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] bytes = ("{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
