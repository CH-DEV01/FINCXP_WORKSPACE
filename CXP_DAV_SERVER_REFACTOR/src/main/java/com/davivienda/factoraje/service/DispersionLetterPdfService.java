package com.davivienda.factoraje.service;

import com.davivienda.factoraje.dto.report.DispersionLetterData;

public interface DispersionLetterPdfService {

    /** Genera el PDF de la carta de solicitud de dispersión de pagos. */
    byte[] generate(DispersionLetterData data);
}
