package com.davivienda.factoraje.infrastructure.security;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * JWT de la petición: del encabezado {@code Authorization: Bearer <token>} o, si no
 * viene, de la cookie de sesión que crea la app de financiamiento.
 */
public final class SessionTokens {

    public static final String COOKIE_NAME = "fincxp_token";
    public static final String CSRF_HEADER = "X-Requested-With";

    private static final String PREFIX = "Bearer ";
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private SessionTokens() {
    }

    public static Optional<String> read(HttpServletRequest request) {
        return fromHeader(request).or(() -> fromCookie(request));
    }

    private static Optional<String> fromHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX) || header.length() == PREFIX.length()) {
            return Optional.empty();
        }
        return Optional.of(header.substring(PREFIX.length()));
    }

    /**
     * El navegador envía la cookie en cualquier petición al dominio, incluso desde otro
     * sitio. Fuera de los métodos de lectura se exige un encabezado propio, que otro
     * origen no puede agregar sin pasar el preflight de CORS.
     */
    private static Optional<String> fromCookie(HttpServletRequest request) {
        if (!SAFE_METHODS.contains(request.getMethod()) && request.getHeader(CSRF_HEADER) == null) {
            return Optional.empty();
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
