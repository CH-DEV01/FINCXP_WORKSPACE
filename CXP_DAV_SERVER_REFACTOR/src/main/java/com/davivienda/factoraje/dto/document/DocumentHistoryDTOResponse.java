package com.davivienda.factoraje.dto.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;

/**
 * @param uploadedBy    nombre del usuario que hizo la carga; solo en la bitácora del pagador.
 * @param inactivatable true si el pagador puede pasarlo manualmente a Inactivo:
 *                      está Cargado (APPROVED)
 */
public record DocumentHistoryDTOResponse(
        UUID id,
        String documentNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate disbursementDate,
        BigDecimal amount,
        BigDecimal disbursedAmount,
        DocumentStatusEnum status,
        UUID payerId,
        String payerName,
        UUID supplierId,
        String supplierName,
        String supplierNit,
        String uploadedBy,
        boolean inactivatable
) {
}
