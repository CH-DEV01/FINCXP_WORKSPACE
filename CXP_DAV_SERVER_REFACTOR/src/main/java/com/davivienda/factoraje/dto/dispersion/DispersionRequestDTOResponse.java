package com.davivienda.factoraje.dto.dispersion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.DispersionBatchModel;
import com.davivienda.factoraje.domain.enums.DispersionBatchStatusEnum;
import com.davivienda.factoraje.infrastructure.util.Money;

/**
 * Solicitud de dispersión de un pagador: los documentos de una fecha de vencimiento.
 * Sin lote está {@link Status#INGRESADO}; con lote refleja el estado del lote.
 * {@code totalAmount} es la suma de los montos de factura.
 */
public record DispersionRequestDTOResponse(
        UUID batchId,
        String batchNumber,
        LocalDate dueDate,
        LocalDate dispersionDate,
        long documentCount,
        long supplierCount,
        BigDecimal totalAmount,
        Status status,
        Instant batchCreatedAt
) {

    public enum Status {
        INGRESADO,
        EN_PROCESO,
        DISPERSADO
    }

    public static DispersionRequestDTOResponse pending(LocalDate dueDate, long documentCount, long supplierCount,
            BigDecimal totalAmount) {
        return new DispersionRequestDTOResponse(null, null, dueDate, dueDate, documentCount, supplierCount,
                Money.round(totalAmount), Status.INGRESADO, null);
    }

    public static DispersionRequestDTOResponse fromBatch(DispersionBatchModel batch, long supplierCount) {
        return new DispersionRequestDTOResponse(
                batch.getId(),
                batch.getBatchNumber(),
                batch.getDueDate(),
                batch.getDispersionDate(),
                batch.getDocumentCount(),
                supplierCount,
                Money.round(batch.getTotalAmount()),
                batch.getStatus() == DispersionBatchStatusEnum.SETTLED ? Status.DISPERSADO : Status.EN_PROCESO,
                batch.getCreatedAt());
    }
}
