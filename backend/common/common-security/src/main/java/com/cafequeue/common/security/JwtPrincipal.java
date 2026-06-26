package com.cafequeue.common.security;

public record JwtPrincipal(String subject, String role) {
}
