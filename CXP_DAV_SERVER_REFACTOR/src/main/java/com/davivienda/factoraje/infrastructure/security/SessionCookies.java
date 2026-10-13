package com.davivienda.factoraje.infrastructure.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Cookie HttpOnly que lleva el JWT: JavaScript no la puede leer. Solo la arma la API;
 * en el ingreso por SSO el portal reenvía el Set-Cookie de activateSession.
 * Sin Max-Age, el navegador la borra al cerrarse; la vigencia real la da el JWT.
 */
@Component
public class SessionCookies {

    public static final String NAME = "FIN_CXP_SESSION";
    private static final String BEARER_PREFIX = "Bearer ";

    /** Ruta pública de la API. Vacía: la ruta de contexto con la que la API recibe la petición. */
    private final String configuredPath;

    public SessionCookies(@Value("${session.cookie.path:}") String configuredPath) {
        this.configuredPath = configuredPath == null ? "" : configuredPath.trim();
    }

    public ResponseCookie create(String jwt, HttpServletRequest request) {
        return base(jwt, request).build();
    }

    public ResponseCookie clear(HttpServletRequest request) {
        return base("", request).maxAge(0).build();
    }

    /** JWT de la cookie o, mientras se despliegan los clientes nuevos, del encabezado Authorization. */
    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return Optional.of(cookie.getValue());
                }
            }
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()));
        }
        return Optional.empty();
    }

    // Secure también en http://localhost: los navegadores lo tratan como contexto seguro.
    private ResponseCookie.ResponseCookieBuilder base(String value, HttpServletRequest request) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(path(request));
    }

    private String path(HttpServletRequest request) {
        if (!configuredPath.isEmpty()) {
            return configuredPath;
        }
        String contextPath = request.getContextPath();
        return contextPath == null || contextPath.isEmpty() ? "/" : contextPath;
    }
}
