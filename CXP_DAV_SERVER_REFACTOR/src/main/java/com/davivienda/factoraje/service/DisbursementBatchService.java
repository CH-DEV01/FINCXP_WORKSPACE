package com.davivienda.factoraje.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.davivienda.factoraje.dto.disbursement_batch.GenerateBatchRequestDTO;

/** Generación de lotes de desembolso y de su carta PDF. */
public interface DisbursementBatchService {

    /**
     * Genera un lote independiente por cada combinación (vencimiento, solicitud,
     * desembolso) seleccionada y devuelve un PDF por lote, indexado por nombre de archivo.
     */
    Map<String, byte[]> generateDisbursementBatches(UUID createdById, String originalFileName, UUID payerId,
            List<GenerateBatchRequestDTO.Group> groups);

    /** Regenera el PDF de un lote ya creado, indexado por nombre de archivo. */
    Map<String, byte[]> regenerateBatchReport(UUID batchId);
}
