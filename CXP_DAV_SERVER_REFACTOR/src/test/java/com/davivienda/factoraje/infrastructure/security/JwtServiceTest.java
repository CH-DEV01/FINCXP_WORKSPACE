package com.davivienda.factoraje.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.SessionExpiredException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final byte[] KEY = new byte[32];
    private static final String SECRET = Base64.getEncoder().encodeToString(KEY);

    @Mock
    private SystemParameters systemParameters;

    private final UserDetails user = User.withUsername("01234567-8").password("x").roles("ADMIN").build();

    @Test
    void signsWithTheSecretStoredInTheDatabase() {
        JwtService service = serviceWith(15, 60);

        String token = service.generateToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("01234567-8");
        assertThat(service.isTokenValid(token, user)).isTrue();
    }

    @Test
    void theTokenLastsAsLongAsTheAllowedInactivity() {
        Claims claims = claims(serviceWith(15, 60).generateToken(user));

        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(15 * 60_000L);
    }

    @Test
    void windowIncludesServerTime() {
        JwtService service = serviceWith(15, 60);
        String token = service.generateToken(user);
        long before = System.currentTimeMillis();

        var window = service.window(token);

        long after = System.currentTimeMillis();
        assertThat(window.issuedAt()).isEqualTo(claims(token).getIssuedAt().getTime());
        assertThat(window.expiresAt()).isEqualTo(claims(token).getExpiration().getTime());
        assertThat(window.serverTime()).isBetween(before, after);
    }

    @Test
    void refreshingKeepsTheLoginTime() {
        JwtService service = serviceWith(15, 60);
        String token = service.generateToken(user);

        String refreshed = service.refreshToken(user, token);

        assertThat(claims(refreshed).get("auth_time", Number.class).longValue())
                .isEqualTo(claims(token).get("auth_time", Number.class).longValue());
        assertThat(service.isTokenValid(refreshed, user)).isTrue();
    }

    @Test
    void theSessionCannotBeExtendedBeyondItsMaximumDuration() {
        JwtService service = serviceWith(15, 10);
        String token = service.generateToken(user);

        Claims claims = claims(token);
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime()).isEqualTo(10 * 60_000L);
        assertThatThrownBy(() -> service.refreshToken(user, token)).isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void aLogoutRevokesTheTokensIssuedBeforeIt() {
        JwtService service = serviceWith(15, 60);
        UserModel loggedOut = mock(UserModel.class);
        when(loggedOut.getUsername()).thenReturn("01234567-8");
        when(loggedOut.isEnabled()).thenReturn(true);
        String token = service.generateToken(loggedOut);

        when(loggedOut.getSessionsRevokedAt()).thenReturn(Instant.now().plusSeconds(1));
        assertThat(service.isTokenValid(token, loggedOut)).isFalse();

        when(loggedOut.getSessionsRevokedAt()).thenReturn(Instant.now().minusSeconds(60));
        assertThat(service.isTokenValid(token, loggedOut)).isTrue();
    }

    @Test
    void refusesToStartWhileTheSecretIsUnconfigured() {
        when(systemParameters.get(SystemParameterKey.JWT_SECRET)).thenReturn(SystemParameterKey.UNCONFIGURED);

        assertThatThrownBy(() -> new JwtService(systemParameters))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void aRotatedSecretRejectsTokensSignedWithThePreviousOne() {
        String rotated = Base64.getEncoder().encodeToString(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
        });
        JwtService service = serviceWith(15, 60);
        String token = service.generateToken(user);

        when(systemParameters.get(SystemParameterKey.JWT_SECRET)).thenReturn(rotated);

        assertThatThrownBy(() -> service.extractUsername(token)).isInstanceOf(JwtException.class);
    }

    private JwtService serviceWith(int idleMinutes, int maxSessionMinutes) {
        when(systemParameters.get(SystemParameterKey.JWT_SECRET)).thenReturn(SECRET);
        when(systemParameters.getInt(SystemParameterKey.SESSION_IDLE_TIMEOUT_MINUTES)).thenReturn(idleMinutes);
        when(systemParameters.getInt(SystemParameterKey.JWT_EXPIRATION_MINUTES)).thenReturn(maxSessionMinutes);
        return new JwtService(systemParameters);
    }

    private static Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(KEY)).build().parseClaimsJws(token).getBody();
    }
}
