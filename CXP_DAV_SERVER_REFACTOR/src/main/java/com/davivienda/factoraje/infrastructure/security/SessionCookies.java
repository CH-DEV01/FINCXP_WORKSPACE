package com.davivienda.factoraje.infrastructure.security;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Cookie de sesión compartida con la app de financiamiento, que la crea al ingresar
 * desde el portal. Nombre, dominio y ruta deben ser los mismos en las dos apps para
 * que la renovación y el cierre reemplacen la misma cookie.
 */
@Component
@RequiredArgsConstructor
public class SessionCookies {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1");

    private final SystemParameters systemParameters;
    private final JwtService jwtService;
    private final SessionTokens sessionTokens;

    public void write(HttpServletRequest request, HttpServletResponse response, String jwt) {
        long seconds = (jwtService.window(jwt).expiresAt() - System.currentTimeMillis()) / 1000;
        add(response, build(request, jwt, Duration.ofSeconds(Math.max(0, seconds))));
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        add(response, build(request, "", Duration.ZERO));
    }

    private ResponseCookie build(HttpServletRequest request, String value, Duration maxAge) {
        String host = request.getServerName().toLowerCase(Locale.ROOT);
        boolean local = LOCAL_HOSTS.contains(host);
        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(sessionTokens.cookieName(), value)
                .path("/")
                .httpOnly(true)
                .secure(!local)
                .sameSite("Lax")
                .maxAge(maxAge);
        // El navegador rechaza una cookie para un dominio que no contiene al host
        // (por ejemplo, en localhost): ahí queda solo para el host.
        String domain = configuredDomain();
        if (host.equals(domain) || host.endsWith("." + domain)) {
            cookie.domain(domain);
        }
        return cookie.build();
    }

    private String configuredDomain() {
        String domain = systemParameters.get(SystemParameterKey.SESSION_COOKIE_DOMAIN).trim().toLowerCase(Locale.ROOT);
        return domain.startsWith(".") ? domain.substring(1) : domain;
    }

    private static void add(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
