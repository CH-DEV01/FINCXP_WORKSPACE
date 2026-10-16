package com.davivienda.factoraje.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.domain.entities.PendingSessionModel;
import com.davivienda.factoraje.repository.PendingSessionRepository;

class PendingSessionServiceTest {

    private final PendingSessionRepository repository = mock(PendingSessionRepository.class);
    private final PendingSessionService service = new PendingSessionService(repository);

    @Test
    void consumeDeletesTheTokenBeforeReturningIt() {
        PendingSessionModel session = new PendingSessionModel("tok-1", "ana@empresa.com",
                LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        when(repository.findById("tok-1")).thenReturn(Optional.of(session));

        PendingSessionModel consumed = service.consume("tok-1");

        assertThat(consumed).isSameAs(session);
        verify(repository).delete(session);
        verify(repository).flush();
    }

    @Test
    void consumeRejectsAMissingToken() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("missing"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("invalid_session");
        verify(repository).findById("missing");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void consumeRejectsABlankTokenWithoutTouchingTheRepository() {
        assertThatThrownBy(() -> service.consume(" "))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("invalid_session");
        assertThatThrownBy(() -> service.consume(null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("invalid_session");
        verifyNoMoreInteractions(repository);
    }
}
