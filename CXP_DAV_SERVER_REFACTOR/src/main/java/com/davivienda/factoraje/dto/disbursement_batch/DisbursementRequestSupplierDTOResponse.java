package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Detalle de una solicitud de desembolso agrupado por proveedor.
 *
 * @param accountNumber  cuenta principal del proveedor a la que se abona
 * @param totalAmount    monto a desembolsar: factura menos intereses
 * @param amountToCredit monto a abonar: factura menos intereses y comisión (IVA incluido)
 */
public record DisbursementRequestSupplierDTOResponse(
        UUID supplierId,
        String supplierName,
        String accountNumber,
        long documentCount,
        BigDecimal totalAmount,
        BigDecimal amountToCredit,
        LocalDate dueDate
) {}
