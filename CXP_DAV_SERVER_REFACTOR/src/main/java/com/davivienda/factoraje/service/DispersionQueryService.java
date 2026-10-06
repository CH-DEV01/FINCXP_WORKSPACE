package com.davivienda.factoraje.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.dispersion.DispersionBatchDetailDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchHistoryDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionDocumentDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionPayerDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionRequestDTOResponse;

public interface DispersionQueryService {

    /** Pagadores con su cuenta principal y las fechas de vencimiento pendientes de dispersar. */
    List<DispersionPayerDTOResponse> getPayers();

    /**
     * Solicitudes del pagador: primero las fechas de vencimiento pendientes (de la más
     * próxima a la más lejana) y luego sus lotes, del más reciente al más antiguo.
     */
    List<DispersionRequestDTOResponse> getPayerRequests(UUID payerId);

    /** Documentos pendientes de dispersar del pagador con el vencimiento dado. */
    List<DispersionDocumentDTOResponse> getPendingDocuments(UUID payerId, LocalDate dueDate);

    DispersionBatchHistoryDTOResponse getBatchHistory(UUID payerId, int page, int size);

    DispersionBatchDetailDTOResponse getBatchDetails(UUID batchId);
}
