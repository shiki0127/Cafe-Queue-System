package com.cafequeue.common.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class JwtTokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final byte[] secret;

    public JwtTokenService(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String issue(String subject, String role, long ttlSeconds) {
        long expiresAt = Instant.now().plusSeconds(ttlSeconds).getEpochSecond();
        String body = subject + ":" + role + ":" + expiresAt;
        String encodedBody = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(body.getBytes(StandardCharsets.UTF_8));
        return encodedBody + "." + sign(encodedBody);
    }

    public JwtPrincipal verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
            throw new IllegalArgumentException("invalid token signature");
        }
        String body = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        String[] fields = body.split(":");
        if (fields.length != 3 || Long.parseLong(fields[2]) < Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("expired token");
        }
        return new JwtPrincipal(fields[0], fields[1]);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("jwt signing failed", ex);
        }
    }
}
