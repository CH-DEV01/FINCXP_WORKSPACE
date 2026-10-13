package com.davivienda.factoraje.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.auth.SessionDTO;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.SessionExpiredException;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Cada token dura lo que la inactividad permitida (SESSION_IDLE_TIMEOUT_MINUTES) y el
 * cliente lo renueva mientras el usuario trabaja. La sesión completa no pasa de
 * JWT_EXPIRATION_MINUTES desde el ingreso, que viaja en el claim auth_time.
 */
@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;
    private static final String AUTH_TIME_CLAIM = "auth_time";

    private final SystemParameters systemParameters;

    public JwtService(SystemParameters systemParameters) {
        this.systemParameters = systemParameters;
        signingKey();
    }

    private SecretKey signingKey() {
        return buildKey(systemParameters.get(SystemParameterKey.JWT_SECRET));
    }

    private static SecretKey buildKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank() || SystemParameterKey.UNCONFIGURED.equals(secretKey.trim())) {
            throw new IllegalStateException(
                    "Falta el parámetro JWT_SECRET en system_parameters "
                            + "(llave en Base64, generar con: openssl rand -base64 32).");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secretKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET no es un valor Base64 válido.", e);
        }
        if (keyBytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 256 bits (32 bytes en Base64).");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Token de una sesión nueva. */
    public String generateToken(UserDetails userDetails) {
        Date now = new Date();
        return issue(userDetails, now, now);
    }

    /**
     * Token nuevo para la misma sesión, que conserva el momento del ingreso.
     *
     * @throws SessionExpiredException si la sesión ya alcanzó su duración máxima.
     */
    public String refreshToken(UserDetails userDetails, String token) {
        Claims claims = parse(token);
        Date authTime = authTime(claims);
        if (!sessionEnd(authTime).after(claims.getExpiration())) {
            throw new SessionExpiredException("La sesión alcanzó su duración máxima. Ingrese nuevamente desde el portal.");
        }
        return issue(userDetails, authTime, new Date());
    }

    private String issue(UserDetails userDetails, Date authTime, Date now) {
        long idleMillis = systemParameters.getInt(SystemParameterKey.SESSION_IDLE_TIMEOUT_MINUTES) * 60_000L;
        long expiration = Math.min(now.getTime() + idleMillis, sessionEnd(authTime).getTime());
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(now)
                .claim(AUTH_TIME_CLAIM, authTime.getTime() / 1000)
                .setExpiration(new Date(expiration))
                .signWith(signingKey())
                .compact();
    }

    private Date sessionEnd(Date authTime) {
        long maxMillis = systemParameters.getInt(SystemParameterKey.JWT_EXPIRATION_MINUTES) * 60_000L;
        return new Date(authTime.getTime() + maxMillis);
    }

    private static Date authTime(Claims claims) {
        Number seconds = claims.get(AUTH_TIME_CLAIM, Number.class);
        return seconds != null ? new Date(seconds.longValue() * 1000) : claims.getIssuedAt();
    }

    public SessionDTO sessionOf(String token) {
        Claims claims = parse(token);
        return new SessionDTO(claims.getIssuedAt().getTime(), claims.getExpiration().getTime());
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parse(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && userDetails.isEnabled()
                && claims.getExpiration().after(new Date())
                && !revoked(claims, userDetails);
    }

    /** El iat está en segundos: un token del mismo segundo del cierre también queda revocado. */
    private static boolean revoked(Claims claims, UserDetails userDetails) {
        return userDetails instanceof UserModel user
                && user.getSessionsRevokedAt() != null
                && claims.getIssuedAt().toInstant().isBefore(user.getSessionsRevokedAt());
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
