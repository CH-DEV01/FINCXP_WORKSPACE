package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.document.DocumentDTOResponse;
import com.davivienda.factoraje.dto.document.DocumentHistoryDTOResponse;
import com.davivienda.factoraje.dto.document.DocumentHistorySummaryDTOResponse;

public interface DocumentService {

    /**
     * Documentos APPROVED de un convenio que aún son financiables: su fecha de
     * vencimiento no cae dentro de los próximos días de gracia configurados.
     */
    List<DocumentDTOResponse> getFinanceableDocumentsByMasterAgreement(UUID masterAgreementId);

    /**
     * Bitácora paginada de documentos de un proveedor, sin los que no llegaron a
     * financiarse (no financiables, inactivados por el pagador y dispersados).
     * Los filtros nulos o vacíos no se aplican; {@code search} busca en el número de documento.
     */
    Page<DocumentHistoryDTOResponse> getDocumentHistoryBySupplier(UUID supplierId, UUID payerId,
            DocumentStatusEnum status, String search, int page, int size);

    /**
     * Bitácora paginada de documentos cargados por un pagador, con el proveedor de cada uno.
     * {@code search} busca en el número de documento y en el nombre del proveedor.
     */
    Page<DocumentHistoryDTOResponse> getDocumentHistoryByPayer(UUID payerId, UUID supplierId,
            DocumentStatusEnum status, String search, int page, int size);

    /** Cantidad y monto de los documentos del pagador por proveedor y estado. */
    List<DocumentHistorySummaryDTOResponse> getDocumentHistorySummaryByPayer(UUID payerId);

    /**
     * Pasa manualmente a Inactivo un documento Cargado del pagador del usuario
     * autenticado, sea financiable o no.
     */
    void inactivateDocumentByPayer(UUID documentId);

    /**
     * Pasa a cuarentena documentos Cargados que ya no son financiables y libera su
     * monto del cupo del pagador. Debe llamarse con los documentos ya bloqueados.
     */
    void quarantineNonFinanceable(List<DocumentModel> documents, UserModel actor);

}
