package com.davivienda.factoraje.infrastructure.exception;

/**
 * La operación no es compatible con el estado de la versión de términos: editar una
 * versión publicada o aceptar una que ya no está vigente.
 */
public class TermVersionConflictException extends ConflictException {

    public TermVersionConflictException(String message) {
        super(message);
    }
}
