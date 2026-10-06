package com.davivienda.factoraje.dto.upload_batch;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceRecordDTO(
    int rowIndex,
    LocalDate issueDate,
    BigDecimal nominalAmount,
    String documentNumber,
    String generationCode,
    String receivedStamp,
    String controlNumber,
    String issuanceMethod,
    String invoiceType,
    String supplierNit,
    String supplierName,
    String supplierAccountNumber,
    String paymentPolicy,
    String disbursementDay
) {}
