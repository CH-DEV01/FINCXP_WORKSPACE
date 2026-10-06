package com.davivienda.factoraje.infrastructure.exception;

/**
 * Base de las operaciones rechazadas por el estado actual de los datos: responde 409.
 */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }

    protected ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
