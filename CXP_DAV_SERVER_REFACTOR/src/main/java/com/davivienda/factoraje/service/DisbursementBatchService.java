package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.disbursement_batch.GenerateBatchRequestDTO;

/** Generación de lotes de desembolso y de su carta PDF. */
public interface DisbursementBatchService {

    record Letter(String fileName, byte[] content) {}

    /**
     * Genera un lote independiente por cada combinación (vencimiento, solicitud,
     * desembolso) seleccionada. Un solo lote → PDF; varios → ZIP con un PDF por lote.
     */
    Letter generateDisbursementBatches(UUID createdById, String originalFileName, UUID payerId,
            List<GenerateBatchRequestDTO.Group> groups);

    /** Regenera el PDF de un lote ya creado. */
    Letter regenerateBatchReport(UUID batchId);
}
