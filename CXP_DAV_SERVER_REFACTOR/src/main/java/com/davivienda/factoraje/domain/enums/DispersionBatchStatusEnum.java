package com.davivienda.factoraje.domain.enums;

public enum DispersionBatchStatusEnum {

    /** Carta generada; el banco aún no confirma la dispersión. */
    CREATED,

    /** Dispersión confirmada: sus documentos quedaron dispersados. */
    SETTLED
}
