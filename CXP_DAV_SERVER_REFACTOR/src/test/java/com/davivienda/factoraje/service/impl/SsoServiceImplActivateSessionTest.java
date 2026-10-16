package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import com.davivienda.factoraje.domain.catalogs.RoleCat;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.PendingSessionModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.JwtService;
import com.davivienda.factoraje.repository.ParameterRepository;
import com.davivienda.factoraje.repository.PendingSessionRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.PendingSessionService;

class SsoServiceImplActivateSessionTest {

    private static final String TOKEN = "11111111-2222-4333-8444-555555555555";

    private final PendingSessionService pendingSessionService = mock(PendingSessionService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final SsoServiceImpl service = new SsoServiceImpl(mock(ParameterRepository.class), mock(RestTemplate.class),
            mock(PendingSessionRepository.class), pendingSessionService, userRepository, jwtService);

    private PendingSessionModel pending;

    @BeforeEach
    void setUp() {
        pending = new PendingSessionModel(TOKEN, "ana@empresa.com",
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(4));
        when(pendingSessionService.consume(TOKEN)).thenReturn(pending);
    }

    @Test
    void aFailedActivationStillConsumesTheTokenSoItCannotBeReused() {
        UserModel inactive = activeUser();
        inactive.setStatus(GeneralStatusEnum.INACTIVE);
        when(userRepository.findByEmail("ana@empresa.com")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.activateSession(TOKEN))
                .isInstanceOf(UnauthorizedAccessException.class);
        verify(pendingSessionService).consume(TOKEN);
        verify(jwtService, never()).generateToken(inactive);

        when(pendingSessionService.consume(TOKEN)).thenThrow(new RuntimeException("invalid_session"));
        assertThatThrownBy(() -> service.activateSession(TOKEN))
                .hasMessage("invalid_session");
    }

    @Test
    void anExpiredTokenIsConsumedAndRejected() {
        pending.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThatThrownBy(() -> service.activateSession(TOKEN))
                .hasMessage("session_expired");
        verify(pendingSessionService).consume(TOKEN);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void aValidTokenIssuesAJwt() {
        UserModel user = activeUser("ana@empresa.com");
        when(userRepository.findByEmail("ana@empresa.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt.ok");

        assertThat(service.activateSession(TOKEN)).isEqualTo("jwt.ok");
        verify(pendingSessionService).consume(TOKEN);
    }

    @Test
    void findsTheRegisteredEmailKeepingCaseAndHyphens() {
        pending.setUserEmail(" Ana-Lopez@Grupo-X.com ");
        UserModel user = activeUser("Ana-Lopez@Grupo-X.com");
        when(userRepository.findByEmail("Ana-Lopez@Grupo-X.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt.hyphen");

        assertThat(service.activateSession(TOKEN)).isEqualTo("jwt.hyphen");
        verify(userRepository).findByEmail("Ana-Lopez@Grupo-X.com");
        verify(userRepository, never()).findByEmail("ana-lopez@grupo-x.com");
        verify(userRepository, never()).findByEmail("analopez@grupox.com");
    }

    private static UserModel activeUser() {
        return activeUser("ana@empresa.com");
    }

    private static UserModel activeUser(String email) {
        RoleCat role = new RoleCat();
        role.setStatus(GeneralStatusEnum.ACTIVE);
        EntityModel entity = new EntityModel();
        entity.setStatus(GeneralStatusEnum.ACTIVE);
        UserModel user = new UserModel();
        user.setEmail(email);
        user.setStatus(GeneralStatusEnum.ACTIVE);
        user.setRole(role);
        user.setEntity(entity);
        return user;
    }
}
