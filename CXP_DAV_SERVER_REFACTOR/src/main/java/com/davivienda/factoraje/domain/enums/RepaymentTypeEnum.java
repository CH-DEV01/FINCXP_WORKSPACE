package com.davivienda.factoraje.domain.enums;

public enum RepaymentTypeEnum {

    PARTIAL,
    FULL,
    // Cargo: consumo previo registrado al dar de alta el cupo, no un abono
    INITIAL_BALANCE,
    // Liberación del monto nominal de un documento que pasó a Inactivo
    DOCUMENT_INACTIVATION

}
