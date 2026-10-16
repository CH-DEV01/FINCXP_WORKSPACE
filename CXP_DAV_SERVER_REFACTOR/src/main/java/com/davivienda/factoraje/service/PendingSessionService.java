package com.davivienda.factoraje.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.PendingSessionModel;
import com.davivienda.factoraje.repository.PendingSessionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Consume el sessionToken del handoff en una transacción propia. Así el borrado
 * queda guardado aunque activateSession falle después (usuario inactivo, vencido, etc.).
 */
@Service
@RequiredArgsConstructor
public class PendingSessionService {

    private final PendingSessionRepository pendingSessionRepository;

    /**
     * Busca el token, lo borra y confirma el borrado antes de devolver la fila en memoria.
     *
     * @throws RuntimeException {@code invalid_session} si el token no existe o viene vacío.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PendingSessionModel consume(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new RuntimeException("invalid_session");
        }
        PendingSessionModel session = pendingSessionRepository.findById(sessionToken)
                .orElseThrow(() -> new RuntimeException("invalid_session"));
        pendingSessionRepository.delete(session);
        pendingSessionRepository.flush();
        return session;
    }
}
