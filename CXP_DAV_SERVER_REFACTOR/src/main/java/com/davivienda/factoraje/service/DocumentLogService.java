package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;

public interface DocumentLogService {

    /**
     * Registra el historial de un lote de documentos de forma masiva (Bulk Insert).
     *
     * @param documents Lista de documentos recién guardados.
     * @param status Estado o acción aplicada a los documentos.
     * @param performedBy UUID del usuario que ejecutó la acción.
     * @param acceptanceAuditId UUID de la auditoría de aceptación legal.
     */
    void createLogsForBatch(
            List<DocumentModel> documents, 
            DocumentStatusEnum status, 
            UUID performedBy, 
            UUID acceptanceAuditId
    );


    
}
