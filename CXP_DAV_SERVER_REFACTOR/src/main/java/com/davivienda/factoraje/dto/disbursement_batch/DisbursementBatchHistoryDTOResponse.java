package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.util.List;

/**
 * Página de la bitácora de lotes. {@code totalAmountToDisburse} suma todos los lotes
 * del filtro, no sólo los de la página.
 */
public record DisbursementBatchHistoryDTOResponse(
        List<DisbursementBatchSummaryDTOResponse> content,
        int number,
        int size,
        int totalPages,
        long totalElements,
        BigDecimal totalAmountToDisburse
) {
}
