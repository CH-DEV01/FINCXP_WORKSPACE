package com.davivienda.factoraje.dto.report;

import java.util.List;

/**
 * Datos ya formateados para la carta de solicitud de desembolso.
 * {@code totalAmount} debe ser la suma de {@code amountToDisburse} de las filas.
 */
public record DisbursementLetterData(
        String batchNumber,
        String requestDate,
        String signerName,
        String signerDui,
        String companyName,
        String creditFacilityReference,
        String totalAmount,
        List<Row> rows
) {

    public record Row(
            String requestDate,
            String financingDays,
            String dueDate,
            String invoiceAmount,
            String interest,
            String amountToDisburse,
            String commissionWithIva,
            String amountToCredit,
            String accountNumber,
            String accountName
    ) {}
}
