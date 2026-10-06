package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchSummaryDTOResponse;

/** Confirmación de desembolsos ejecutados. */
public interface DisbursementConfirmationService {

    /**
     * Confirma el lote completo: todos sus documentos pasan a DISBURSED y el
     * lote queda SETTLED.
     */
    DisbursementBatchSummaryDTOResponse confirmDisbursementBatch(UUID batchId, UUID confirmedById);

    void confirmDisbursement(List<UUID> documentIds, UUID confirmedById);
}
