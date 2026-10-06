package com.davivienda.factoraje.dto.disbursement_batch;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;

/**
 * @param disbursementDate fecha de desembolso programada al momento de la solicitud
 * @param dueDate          fecha de vencimiento común a los documentos del lote
 * @param requestDate      fecha de solicitud común a los documentos del lote
 */
public record DisbursementBatchDetailDTOResponse(
        UUID id,
        String batchNumber,
        String outputFileName,
        Integer documentCount,
        BigDecimal totalAmount,
        BigDecimal totalCommission,
        BigDecimal totalInterest,
        DisbursementBatchStatusEnum status,
        String payerName,
        Instant createdAt,
        LocalDate disbursementDate,
        LocalDate dueDate,
        LocalDate requestDate,
        List<DisbursementBatchDocumentDTOResponse> documents
) {}
