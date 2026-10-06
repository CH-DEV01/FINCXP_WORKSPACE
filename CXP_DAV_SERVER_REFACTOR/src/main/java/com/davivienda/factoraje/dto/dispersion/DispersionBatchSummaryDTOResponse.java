package com.davivienda.factoraje.dto.dispersion;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.davivienda.factoraje.domain.entities.DispersionBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DispersionBatchStatusEnum;
import com.davivienda.factoraje.infrastructure.util.Money;

/** Lote de dispersión en la bitácora. {@code totalAmount} es la suma de los montos de factura. */
public record DispersionBatchSummaryDTOResponse(
        UUID id,
        String batchNumber,
        UUID payerId,
        String payerName,
        String payerAccountNumber,
        LocalDate dueDate,
        LocalDate dispersionDate,
        Integer documentCount,
        BigDecimal totalAmount,
        DispersionBatchStatusEnum status,
        String createdByName,
        String confirmedByName,
        Instant createdAt,
        Instant confirmedAt
) {

    public static DispersionBatchSummaryDTOResponse fromEntity(DispersionBatchModel batch) {
        return new DispersionBatchSummaryDTOResponse(
                batch.getId(),
                batch.getBatchNumber(),
                batch.getPayer().getId(),
                batch.getPayer().getName(),
                batch.getPayerAccountNumber(),
                batch.getDueDate(),
                batch.getDispersionDate(),
                batch.getDocumentCount(),
                Money.round(batch.getTotalAmount()),
                batch.getStatus(),
                fullName(batch.getCreatedBy()),
                fullName(batch.getConfirmedBy()),
                batch.getCreatedAt(),
                batch.getConfirmedAt());
    }

    public static String fullName(UserModel user) {
        if (user == null) {
            return null;
        }
        return Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }
}
