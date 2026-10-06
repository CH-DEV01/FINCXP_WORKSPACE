package com.davivienda.factoraje.infrastructure.exception;

public class ResourceAlreadyExistsException extends ConflictException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
