package com.davivienda.financiamiento.config;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.davivienda.financiamiento.domain.entities.SystemParameterModel;
import com.davivienda.financiamiento.repository.ParameterRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Agrega las cabeceras de seguridad y la Content Security Policy a todas las
 * respuestas del Web Server.
 *
 * La política es fija; por ambiente solo cambian el modo (CSP_MODE) y los orígenes
 * de la Core API a los que el navegador puede llamar (CSP_CONNECT_SRC), porque
 * system_parameters no admite valores de más de 255 caracteres.
 */
@Slf4j
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    static final String ENFORCE_HEADER = "Content-Security-Policy";
    static final String REPORT_ONLY_HEADER = "Content-Security-Policy-Report-Only";

    static final String MODE_KEY = "CSP_MODE";
    static final String CONNECT_SRC_KEY = "CSP_CONNECT_SRC";

    // Sin orígenes configurados se permite cualquier origen https: la URL de la Core API
    // cambia por ambiente y bloquearla dejaría la aplicación sin datos.
    static final String DEFAULT_CONNECT_SRC = "https:";

    static final Duration CACHE_TTL = Duration.ofSeconds(60);

    private static final Pattern ORIGIN = Pattern.compile("https?://[a-z0-9.-]+(:\\d{1,5})?");

    enum Mode {
        REPORT_ONLY, ENFORCE, DISABLED
    }

    private record Header(String name, String value, Instant loadedAt) {
    }

    private final ParameterRepository parameterRepository;
    private final Clock clock;

    private volatile Header cached;

    @Autowired
    public SecurityHeadersFilter(ParameterRepository parameterRepository) {
        this(parameterRepository, Clock.systemUTC());
    }

    SecurityHeadersFilter(ParameterRepository parameterRepository, Clock clock) {
        this.parameterRepository = parameterRepository;
        this.clock = clock;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        Header header = header();
        if (header.name() != null) {
            response.setHeader(header.name(), header.value());
        }
        chain.doFilter(request, response);
    }

    /** Las rutas del frontend llegan como 404 y SpaErrorController responde index.html. */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    static String policy(String connectSrc) {
        return String.join("; ",
                "default-src 'self'",
                "script-src 'self'",
                // SweetAlert2 inyecta <style> en tiempo de ejecución y React usa style={...}.
                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
                "font-src 'self' https://fonts.gstatic.com",
                "img-src 'self' data: blob:",
                "connect-src 'self' " + connectSrc,
                "frame-ancestors 'self' https://*.davivienda.com.sv",
                "form-action 'self'",
                "base-uri 'self'",
                "object-src 'none'");
    }

    private Header header() {
        Header current = cached;
        Instant now = clock.instant();
        if (current == null || !now.isBefore(current.loadedAt().plus(CACHE_TTL))) {
            current = load(now);
            cached = current;
        }
        return current;
    }

    private Header load(Instant now) {
        Mode mode;
        String connectSrc;
        try {
            mode = mode();
            connectSrc = connectSrc();
        } catch (RuntimeException e) {
            // Sin base de datos el frontend se sigue sirviendo, con la política en modo reporte.
            log.warn("[csp] No se pudieron leer los parámetros de la CSP: {}", e.getMessage());
            mode = Mode.REPORT_ONLY;
            connectSrc = DEFAULT_CONNECT_SRC;
        }
        return switch (mode) {
            case ENFORCE -> new Header(ENFORCE_HEADER, policy(connectSrc), now);
            case REPORT_ONLY -> new Header(REPORT_ONLY_HEADER, policy(connectSrc), now);
            case DISABLED -> new Header(null, null, now);
        };
    }

    private Mode mode() {
        String value = parameterRepository.findByKey(MODE_KEY)
                .map(SystemParameterModel::getValue)
                .map(raw -> raw.trim().toUpperCase(Locale.ROOT))
                .orElse(Mode.REPORT_ONLY.name());
        try {
            return Mode.valueOf(value);
        } catch (IllegalArgumentException e) {
            log.warn("[csp] {} inválido ({}); se usa {}", MODE_KEY, value, Mode.REPORT_ONLY);
            return Mode.REPORT_ONLY;
        }
    }

    /** Solo pasan orígenes válidos: un ';' en el valor agregaría directivas a la política. */
    private String connectSrc() {
        String value = parameterRepository.findByKey(CONNECT_SRC_KEY)
                .map(SystemParameterModel::getValue)
                .orElse("");
        String origins = Arrays.stream(value.split("[,\\s]+"))
                .map(token -> token.trim().toLowerCase(Locale.ROOT))
                .map(token -> token.endsWith("/") ? token.substring(0, token.length() - 1) : token)
                .filter(token -> !token.isEmpty())
                .filter(token -> {
                    boolean valid = ORIGIN.matcher(token).matches();
                    if (!valid && !"configurar".equals(token)) {
                        log.warn("[csp] Origen inválido en {}: {}", CONNECT_SRC_KEY, token);
                    }
                    return valid;
                })
                .distinct()
                .collect(Collectors.joining(" "));
        return origins.isEmpty() ? DEFAULT_CONNECT_SRC : origins;
    }
}
