package com.davivienda.factoraje.infrastructure.exception;

/**
 * Usuario autenticado sin permiso sobre el recurso: responde 403. El 401 queda
 * reservado para sesión ausente o vencida, que el cliente trata cerrando la sesión.
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}