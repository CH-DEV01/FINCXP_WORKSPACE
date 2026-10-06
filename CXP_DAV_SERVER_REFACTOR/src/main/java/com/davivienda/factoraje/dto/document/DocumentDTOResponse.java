package com.davivienda.factoraje.dto.document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.InvoiceTypeEnum;
import com.davivienda.factoraje.domain.enums.IssuanceMethodEnum;

public record DocumentDTOResponse(

        UUID id,
        String documentNumber,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal nominalAmount,
        String generationCode,
        String receivedStamp,
        String controlNumber,
        IssuanceMethodEnum issuanceMethod,
        InvoiceTypeEnum invoiceType,
        DocumentStatusEnum status,
        UUID masterAgreementId,
        UUID uploadBatchId

) {

    public static DocumentDTOResponse fromEntity(DocumentModel model){

        return new DocumentDTOResponse(
            model.getId(),
            model.getDocumentNumber(), 
            model.getIssueDate(), 
            model.getDueDate(),
            model.getNominalAmount(),
            model.getGenerationCode(), 
            model.getReceivedStamp(), 
            model.getControlNumber(), 
            model.getIssuanceMethod(),
            model.getInvoiceType(), 
            model.getStatus(), 
            model.getMasterAgreement().getId(), 
            model.getUploadBatch().getId());
    }

}
