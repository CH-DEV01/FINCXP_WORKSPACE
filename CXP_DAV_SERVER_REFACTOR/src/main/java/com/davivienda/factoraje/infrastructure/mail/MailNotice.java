package com.davivienda.factoraje.infrastructure.mail;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Avisos que se envían después de confirmar la operación. */
public sealed interface MailNotice {

    record SupplierDocuments(String nit, String supplierName, int documentCount) {
    }

    record PayerUploadSucceeded(
            UUID payerId,
            String payerName,
            UUID uploadBatchId,
            String fileName,
            int documentCount,
            String uploadedBy,
            List<SupplierDocuments> suppliers,
            Instant occurredAt) implements MailNotice {
    }

    record UploadFailed(
            UUID payerId,
            String payerName,
            String fileName,
            String uploadedBy,
            String reason) implements MailNotice {
    }

    record OperatorUploadSucceeded(
            UUID payerId,
            String payerName,
            UUID uploadBatchId,
            String fileName,
            int documentCount,
            String operatorName,
            Instant occurredAt) implements MailNotice {
    }

    record FundingRequested(
            String requestNumber,
            String supplierName,
            String payerName,
            String requestedBy,
            int documentCount,
            BigDecimal totalAmount,
            BigDecimal totalAmountToDisburse,
            LocalDate disbursementDate,
            Instant occurredAt) implements MailNotice {
    }

    record DisbursementBatchCreated(
            String batchNumber,
            String payerName,
            LocalDate dueDate,
            LocalDate requestDate,
            LocalDate disbursementDate,
            int documentCount,
            Instant occurredAt) implements MailNotice {
    }

    record DispersionBatchCreated(
            String batchNumber,
            String payerName,
            LocalDate dueDate,
            int documentCount,
            Instant occurredAt) implements MailNotice {
    }

    record OperatorChange(
            String actorName,
            String menu,
            String previousValue,
            String newValue,
            Instant occurredAt) implements MailNotice {
    }
}
