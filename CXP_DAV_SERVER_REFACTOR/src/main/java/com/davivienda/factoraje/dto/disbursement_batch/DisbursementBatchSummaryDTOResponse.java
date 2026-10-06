package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;
import com.davivienda.factoraje.infrastructure.util.Money;

/**
 * Lote de la bitácora. {@code totalAmountToDisburse} es el monto a desembolsar
 * (factura menos intereses), calculado igual que el total de la carta.
 */
public record DisbursementBatchSummaryDTOResponse(
        UUID id,
        String batchNumber,
        String outputFileName,
        Integer documentCount,
        BigDecimal totalAmountToDisburse,
        BigDecimal totalCommission,
        BigDecimal totalInterest,
        DisbursementBatchStatusEnum status,
        String createdByName,
        String confirmedByName,
        UUID payerId,
        String payerName,
        LocalDate dueDate,
        LocalDate requestDate,
        LocalDate disbursementDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static DisbursementBatchSummaryDTOResponse fromEntity(DisbursementBatchModel model,
            BigDecimal totalAmountToDisburse) {
        return new DisbursementBatchSummaryDTOResponse(
                model.getId(),
                model.getBatchNumber(),
                model.getOutputFileName(),
                model.getTransactionCount(),
                totalAmountToDisburse,
                Money.round(model.getTotalCommission()),
                Money.round(model.getTotalInterest()),
                model.getStatus(),
                fullName(model.getCreatedBy()),
                fullName(model.getConfirmedBy()),
                model.getPayer() != null ? model.getPayer().getId() : null,
                model.getPayer() != null ? model.getPayer().getName() : null,
                model.getDueDate(),
                model.getRequestDate(),
                model.getDisbursementDate(),
                model.getCreatedAt(),
                model.getUpdatedAt());
    }

    private static String fullName(UserModel user) {
        if (user == null) {
            return null;
        }
        return (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
