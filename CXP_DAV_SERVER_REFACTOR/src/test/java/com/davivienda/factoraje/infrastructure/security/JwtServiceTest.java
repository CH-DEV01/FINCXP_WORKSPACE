package com.davivienda.factoraje.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;

import io.jsonwebtoken.JwtException;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private SystemParameters systemParameters;

    @Test
    void signsWithTheSecretStoredInTheDatabase() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        when(systemParameters.get(SystemParameterKey.JWT_SECRET)).thenReturn(secret);
        when(systemParameters.getInt(SystemParameterKey.JWT_EXPIRATION_MINUTES)).thenReturn(60);

        JwtService service = new JwtService(systemParameters);
        UserDetails user = User.withUsername("01234567-8").password("x").roles("ADMIN").build();

        String token = service.generateToken(user);

        assertThat(service.extractUsername(token)).isEqualTo("01234567-8");
        assertThat(service.isTokenValid(token, user)).isTrue();
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
        String current = Base64.getEncoder().encodeToString(new byte[32]);
        String rotated = Base64.getEncoder().encodeToString(new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32
        });
        when(systemParameters.get(SystemParameterKey.JWT_SECRET)).thenReturn(current);
        when(systemParameters.getInt(SystemParameterKey.JWT_EXPIRATION_MINUTES)).thenReturn(60);

        JwtService service = new JwtService(systemParameters);
        UserDetails user = User.withUsername("01234567-8").password("x").roles("ADMIN").build();
        String token = service.generateToken(user);

        when(systemParameters.get(SystemParameterKey.JWT_SECRET)).thenReturn(rotated);

        assertThatThrownBy(() -> service.extractUsername(token)).isInstanceOf(JwtException.class);
    }
}
