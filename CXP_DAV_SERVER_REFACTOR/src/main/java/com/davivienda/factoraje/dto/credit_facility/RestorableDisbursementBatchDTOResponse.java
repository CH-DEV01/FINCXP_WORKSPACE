package com.davivienda.factoraje.dto.credit_facility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lote de desembolso liquidado cuyo monto aún no se ha abonado al cupo.
 *
 * @param nominalAmount   monto que se restablece: valor nominal de los documentos del lote.
 * @param disbursedAmount monto que se desembolsó al proveedor (nominal menos intereses y comisión).
 */
public record RestorableDisbursementBatchDTOResponse(
        UUID id,
        String batchNumber,
        LocalDate dueDate,
        LocalDate disbursementDate,
        Long documentCount,
        BigDecimal nominalAmount,
        BigDecimal disbursedAmount) {
}
