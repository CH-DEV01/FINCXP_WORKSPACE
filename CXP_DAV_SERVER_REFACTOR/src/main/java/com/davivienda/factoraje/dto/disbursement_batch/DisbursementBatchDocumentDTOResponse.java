package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.QuarantineReasonEnum;
import com.davivienda.factoraje.infrastructure.util.Money;

public record DisbursementBatchDocumentDTOResponse(
        UUID id,
        String documentNumber,
        String supplierName,
        BigDecimal amount,
        BigDecimal amountToFinance,
        BigDecimal interests,
        BigDecimal commissions,
        BigDecimal amountToBeDisbursed,
        LocalDate dueDate,
        LocalDate scheduledDisbursementDate,
        DocumentStatusEnum status,
        QuarantineReasonEnum quarantineReason
) {

    public static DisbursementBatchDocumentDTOResponse fromTransaction(FinancingTransactionModel tx) {
        var document = tx.getDocument();
        String documentNumber = document.getDocumentNumber() != null
                ? document.getDocumentNumber()
                : document.getControlNumber();

        return new DisbursementBatchDocumentDTOResponse(
                document.getId(),
                documentNumber,
                document.getMasterAgreement().getSupplier().getName(),
                Money.round(document.getNominalAmount()),
                Money.round(tx.getAmountToFinance()),
                Money.round(tx.getInterestAmount()),
                Money.round(tx.getCommissionAmount()),
                Money.round(tx.getAmountToBeDisbursed()),
                document.getDueDate(),
                tx.getScheduledDisbursementDate(),
                document.getStatus(),
                document.getQuarantineReason());
    }
}
