package com.davivienda.factoraje.infrastructure.exception;

public class DocumentNotAvailableException extends ConflictException {

    public DocumentNotAvailableException(String message) {
        super(message);
    }

    public DocumentNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
