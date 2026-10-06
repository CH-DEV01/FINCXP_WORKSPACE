package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Combinación (vencimiento, solicitud, desembolso) de documentos de un pagador
 * pendientes de lote. Cada combinación se convierte en un lote independiente.
 * {@code totalAmountToDisburse} se calcula igual que el total de la carta.
 */
public record DisbursementGroupDTOResponse(
        LocalDate dueDate,
        LocalDate requestDate,
        LocalDate disbursementDate,
        long documentCount,
        long supplierCount,
        BigDecimal totalNominalAmount,
        BigDecimal totalAmountToDisburse
) {}
