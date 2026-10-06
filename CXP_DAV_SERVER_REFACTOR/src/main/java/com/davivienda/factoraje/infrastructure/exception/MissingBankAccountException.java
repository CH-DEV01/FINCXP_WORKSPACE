package com.davivienda.factoraje.infrastructure.exception;

/**
 * La entidad no tiene cuenta principal a la cual abonar o cargar la operación.
 */
public class MissingBankAccountException extends ConflictException {

    public MissingBankAccountException(String message) {
        super(message);
    }
}
