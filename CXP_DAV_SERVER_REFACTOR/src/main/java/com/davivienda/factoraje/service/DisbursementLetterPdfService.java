package com.davivienda.factoraje.service;

import com.davivienda.factoraje.dto.report.DisbursementLetterData;

public interface DisbursementLetterPdfService {

    /** Genera el PDF de la carta de solicitud de desembolso. */
    byte[] generate(DisbursementLetterData data);
}
