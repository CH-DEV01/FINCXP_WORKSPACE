package com.davivienda.factoraje.dto.report;

import java.util.List;

/** Datos ya formateados de la carta de solicitud de dispersión de pagos. */
public record DispersionLetterData(
        String batchNumber,
        String dispersionDate,
        String signerName,
        String signerDui,
        String companyName,
        String payerAccountNumber,
        List<Row> rows
) {

    /** Una fila por proveedor y fecha de vencimiento; el monto es la suma de sus facturas. */
    public record Row(
            String dispersionDate,
            String accountName,
            String accountNumber,
            String recordCount,
            String dueDate,
            String invoiceAmount
    ) {}
}
