package com.davivienda.factoraje.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final int MIN_KEY_BYTES = 32;

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

    public String generateToken(UserDetails userDetails) {
        long now = System.currentTimeMillis();
        long expirationMillis = systemParameters.getInt(SystemParameterKey.JWT_EXPIRATION_MINUTES) * 60_000L;
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expirationMillis))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername())
                && userDetails.isEnabled()
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }
}
