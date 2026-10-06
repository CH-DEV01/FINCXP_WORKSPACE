package com.davivienda.factoraje.infrastructure.exception;

import java.math.BigDecimal;

import com.davivienda.factoraje.infrastructure.util.Money;

import lombok.Getter;

/** La carga haría que el pagador supere el límite de su línea de crédito; no se guarda nada. */
@Getter
public class CreditLimitExceededException extends RuntimeException {

    private final BigDecimal batchAmount;
    private final BigDecimal availableAmount;

    public CreditLimitExceededException(BigDecimal batchAmount, BigDecimal availableAmount) {
        super(String.format(
                "Carga rechazada: El archivo totaliza %s y supera el límite de crédito disponible del Pagador (%s).",
                Money.format(batchAmount), Money.format(availableAmount)));
        this.batchAmount = batchAmount;
        this.availableAmount = availableAmount;
    }
}
