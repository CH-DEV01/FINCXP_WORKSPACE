package com.davivienda.factoraje.infrastructure.security;

import java.util.Optional;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;

/** JWT del encabezado {@code Authorization: Bearer <token>}. */
public final class BearerTokens {

    private static final String PREFIX = "Bearer ";

    private BearerTokens() {
    }

    public static Optional<String> read(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX) || header.length() == PREFIX.length()) {
            return Optional.empty();
        }
        return Optional.of(header.substring(PREFIX.length()));
    }
}
