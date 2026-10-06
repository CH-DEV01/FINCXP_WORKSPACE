package com.davivienda.factoraje.domain.enums;

public enum DisbursementPolicyTypeEnum {

    /** Desembolso N días hábiles después de la solicitud (offsetDays = N). */
    T_PLUS_N,

    /**
     * Desembolso en el primer día permitido (weekdays) que caiga al menos
     * offsetDays días hábiles después de la solicitud.
     */
    WEEKDAYS

}
