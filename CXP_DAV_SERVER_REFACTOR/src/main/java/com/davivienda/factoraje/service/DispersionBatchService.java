package com.davivienda.factoraje.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

import com.davivienda.factoraje.dto.dispersion.DispersionBatchSummaryDTOResponse;

public interface DispersionBatchService {

    record Letter(String fileName, byte[] content) {}

    /**
     * Crea el lote con los documentos pendientes de dispersar del pagador con ese
     * vencimiento, los pasa a "En dispersión" y devuelve la carta. La dispersión se
     * realiza en la misma fecha de vencimiento.
     */
    Letter generateBatch(UUID createdById, UUID payerId, LocalDate dueDate);

    /**
     * Genera un lote por cada vencimiento indicado del pagador, en una sola transacción (si uno
     * falla no se crea ninguno), y devuelve un ZIP con la carta de cada lote.
     */
    Letter generateBatches(UUID createdById, UUID payerId, Collection<LocalDate> dueDates);

    /** Vuelve a generar la carta de un lote existente. */
    Letter regenerateLetter(UUID batchId);

    /** El banco confirma la dispersión: todos los documentos del lote quedan dispersados. */
    DispersionBatchSummaryDTOResponse confirmBatch(UUID batchId, UUID confirmedById);
}
