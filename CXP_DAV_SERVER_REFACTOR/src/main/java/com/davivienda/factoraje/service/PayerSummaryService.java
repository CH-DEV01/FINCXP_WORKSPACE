package com.davivienda.factoraje.service;

import java.util.UUID;

import com.davivienda.factoraje.dto.payer.PayerSummaryDTOResponse;

public interface PayerSummaryService {

    /** Resumen de la entidad del usuario autenticado; nunca de otro pagador. */
    PayerSummaryDTOResponse getOwnSummary();

    /** Resumen de cualquier pagador, para el operador bancario. */
    PayerSummaryDTOResponse getSummary(UUID payerId);
}
