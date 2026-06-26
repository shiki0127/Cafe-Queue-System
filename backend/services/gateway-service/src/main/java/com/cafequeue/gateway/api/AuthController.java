package com.cafequeue.gateway.api;

import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.common.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JwtTokenService jwtTokenService;
    private final long ttlSeconds;

    public AuthController(
            @Value("${cafequeue.security.jwt-secret}") String secret,
            @Value("${cafequeue.security.token-ttl-seconds:7200}") long ttlSeconds
    ) {
        this.jwtTokenService = new JwtTokenService(secret);
        this.ttlSeconds = ttlSeconds;
    }

    @PostMapping("/token")
    public ApiResponse<TokenResponse> issue(@RequestBody LoginRequest request) {
        String role = request.role() == null || request.role().isBlank() ? "STUDENT" : request.role();
        String token = jwtTokenService.issue(request.studentId(), role, ttlSeconds);
        return ApiResponse.ok(new TokenResponse(token, ttlSeconds));
    }

    public record LoginRequest(String studentId, String role) {
    }

    public record TokenResponse(String accessToken, long expiresIn) {
    }
}
