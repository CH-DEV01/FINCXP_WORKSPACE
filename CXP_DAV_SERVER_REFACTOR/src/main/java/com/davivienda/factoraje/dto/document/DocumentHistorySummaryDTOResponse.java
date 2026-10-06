package com.davivienda.factoraje.dto.document;

import java.math.BigDecimal;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;

/** Cantidad y monto nominal de los documentos de un pagador por proveedor y estado. */
public record DocumentHistorySummaryDTOResponse(
        UUID supplierId,
        String supplierName,
        String supplierNit,
        DocumentStatusEnum status,
        Long count,
        BigDecimal amount
) {
}
