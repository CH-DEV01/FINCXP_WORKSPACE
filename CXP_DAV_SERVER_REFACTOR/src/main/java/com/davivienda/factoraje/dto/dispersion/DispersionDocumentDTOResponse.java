package com.davivienda.factoraje.dto.dispersion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;

/**
 * Documento de una solicitud o lote de dispersión.
 *
 * @param dteNumber     número de control del DTE o, si no tiene, el número de documento.
 * @param accountNumber cuenta principal del proveedor; nula si no tiene.
 */
public record DispersionDocumentDTOResponse(
        UUID id,
        String dteNumber,
        UUID supplierId,
        String supplierName,
        String accountNumber,
        LocalDate dueDate,
        BigDecimal nominalAmount,
        DocumentStatusEnum status
) {}
