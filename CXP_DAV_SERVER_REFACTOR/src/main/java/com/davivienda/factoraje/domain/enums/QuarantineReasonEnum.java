package com.davivienda.factoraje.domain.enums;

import java.time.LocalDate;

public enum QuarantineReasonEnum {

    /** El documento venció sin ser solicitado. */
    DUE_DATE_EXPIRED,

    /**
     * El documento no fue solicitado y ya no puede financiarse: vence dentro del
     * período de gracia posterior a la próxima fecha de desembolso de su convenio.
     */
    NEAR_DUE_DATE_UNREQUESTED,

    /** El core bancario no pudo procesar el desembolso y el operador decidió no reintentar. */
    DISBURSEMENT_FAILED,

    /** Marcado manualmente para revisión. */
    MANUAL_REVIEW;

    /** Motivo para un documento APPROVED no financiable, según si ya venció a la fecha de referencia. */
    public static QuarantineReasonEnum forUnrequested(LocalDate dueDate, LocalDate referenceDate) {
        return dueDate != null && dueDate.isBefore(referenceDate)
                ? DUE_DATE_EXPIRED
                : NEAR_DUE_DATE_UNREQUESTED;
    }
}
