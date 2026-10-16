package com.davivienda.factoraje.infrastructure.exception;

/**
 * La fecha de desembolso o el monto a desembolsar cambiaron entre la simulación que
 * el proveedor aceptó y el momento de registrar la solicitud (por ejemplo, al cruzar
 * la hora de corte). Responde 409 para que el cliente muestre el nuevo cálculo.
 */
public class FundingQuoteChangedException extends ConflictException {

    public FundingQuoteChangedException(String message) {
        super(message);
    }
}
