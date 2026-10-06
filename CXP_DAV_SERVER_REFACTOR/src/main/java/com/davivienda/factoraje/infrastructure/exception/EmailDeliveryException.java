package com.davivienda.factoraje.infrastructure.exception;

/** Mailjet rechazó el envío o no se pudo contactar. No deshace la operación de negocio. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
