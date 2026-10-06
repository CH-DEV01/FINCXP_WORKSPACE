package com.davivienda.factoraje.domain.enums;

public enum DocumentStatusEnum {

    APPROVED,
    REQUESTED_FOR_FINANCING,
    REQUESTED_FOR_DISBURSEMENT,
    DISBURSED,

    /** El sistema lo retiró por no financiable: vencido o dentro de los días de gracia. */
    IN_QUARANTINE,

    /** El pagador lo pasó manualmente a Inactivo (contingencia). */
    INACTIVATED_BY_PAYER,

    /** No financiable incluido en un lote de dispersión; el pago al proveedor está pendiente de confirmar. */
    REQUESTED_FOR_DISPERSION,

    /** El banco confirmó el pago al proveedor con cargo a la cuenta del pagador. */
    DISPERSED

}
