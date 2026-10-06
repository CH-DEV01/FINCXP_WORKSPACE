package com.davivienda.factoraje.dto.dispersion;

import java.math.BigDecimal;
import java.util.List;

/** Página de la bitácora de dispersiones; {@code totalAmount} suma todos los lotes del filtro. */
public record DispersionBatchHistoryDTOResponse(
        List<DispersionBatchSummaryDTOResponse> content,
        int number,
        int size,
        int totalPages,
        long totalElements,
        BigDecimal totalAmount
) {}
