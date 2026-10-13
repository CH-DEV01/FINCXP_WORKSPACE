package com.davivienda.factoraje.infrastructure.exception;

/**
 * La sesión ya no se puede renovar: responde 401 y el cliente vuelve al portal.
 */
public class SessionExpiredException extends RuntimeException {

    public SessionExpiredException(String message) {
        super(message);
    }
}
