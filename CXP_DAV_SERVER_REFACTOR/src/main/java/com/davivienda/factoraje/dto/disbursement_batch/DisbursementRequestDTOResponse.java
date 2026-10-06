package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;

/**
 * Solicitud de desembolso de un pagador: una combinación (vencimiento, solicitud,
 * desembolso). Sin lote está {@link Status#INGRESADO}; con lote refleja el estado del lote.
 * {@code totalAmount} es el monto a desembolsar, calculado igual que el total de la carta.
 */
public record DisbursementRequestDTOResponse(
        UUID batchId,
        String batchNumber,
        LocalDate dueDate,
        LocalDate requestDate,
        LocalDate disbursementDate,
        long documentCount,
        long supplierCount,
        BigDecimal totalAmount,
        Status status,
        Instant batchCreatedAt
) {

    public enum Status {
        INGRESADO,
        EN_PROCESO,
        DESEMBOLSADO
    }

    public static DisbursementRequestDTOResponse fromGroup(DisbursementGroupDTOResponse group) {
        return new DisbursementRequestDTOResponse(
                null,
                null,
                group.dueDate(),
                group.requestDate(),
                group.disbursementDate(),
                group.documentCount(),
                group.supplierCount(),
                group.totalAmountToDisburse(),
                Status.INGRESADO,
                null);
    }

    public static DisbursementRequestDTOResponse fromBatch(DisbursementBatchModel batch, long supplierCount,
            BigDecimal totalAmountToDisburse) {
        return new DisbursementRequestDTOResponse(
                batch.getId(),
                batch.getBatchNumber(),
                batch.getDueDate(),
                batch.getRequestDate(),
                batch.getDisbursementDate(),
                batch.getTransactionCount() == null ? 0 : batch.getTransactionCount(),
                supplierCount,
                totalAmountToDisburse,
                batch.getStatus() == DisbursementBatchStatusEnum.SETTLED ? Status.DESEMBOLSADO : Status.EN_PROCESO,
                batch.getCreatedAt());
    }
}
