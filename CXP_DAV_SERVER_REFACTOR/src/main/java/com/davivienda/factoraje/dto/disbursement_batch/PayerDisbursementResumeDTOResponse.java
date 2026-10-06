package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param availableRequests combinaciones (vencimiento, solicitud, desembolso) pendientes de lote
 */
public record PayerDisbursementResumeDTOResponse(
        UUID id,
        String name,
        String creditLineNumber,
        BigDecimal creditLineAvailableAmount,
        long availableRequests
) {}
