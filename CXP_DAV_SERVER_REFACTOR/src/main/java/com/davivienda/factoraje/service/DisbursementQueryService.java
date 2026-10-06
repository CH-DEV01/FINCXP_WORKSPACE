package com.davivienda.factoraje.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchDetailDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchHistoryDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementGroupDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestSupplierDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.PayerDisbursementResumeDTOResponse;

/** Consultas de la terminal de desembolsos. */
public interface DisbursementQueryService {

    List<PayerDisbursementResumeDTOResponse> getPayersResume();

    /** Bitácora de lotes paginada, del más reciente al más antiguo; si se indica pagador, sólo los suyos. */
    DisbursementBatchHistoryDTOResponse getBatchHistory(UUID payerId, int page, int size);

    /**
     * Solicitudes del pagador: primero las combinaciones listas sin lote
     * (Ingresado) y luego sus lotes, del más reciente al más antiguo.
     */
    List<DisbursementRequestDTOResponse> getPayerRequests(UUID payerId);

    /**
     * Detalle de una solicitud agrupado por proveedor. Con {@code batchId} se toma
     * el lote; sin él, la combinación (vencimiento, solicitud, desembolso) aún sin lote.
     */
    List<DisbursementRequestSupplierDTOResponse> getRequestSuppliers(UUID payerId, UUID batchId,
            LocalDate dueDate, LocalDate requestDate, LocalDate disbursementDate);

    DisbursementBatchDetailDTOResponse getBatchDetails(UUID batchId);

    /**
     * Combinaciones (vencimiento, solicitud, desembolso) del pagador pendientes de
     * lote. Pueden generarse antes de su fecha de desembolso: el operador es
     * responsable de ejecutarlas en la fecha indicada.
     */
    List<DisbursementGroupDTOResponse> getDisbursementGroups(UUID payerId);
}
