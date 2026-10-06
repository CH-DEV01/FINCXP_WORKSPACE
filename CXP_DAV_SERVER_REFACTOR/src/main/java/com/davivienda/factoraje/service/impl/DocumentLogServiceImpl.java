package com.davivienda.factoraje.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.entities.AcceptanceAuditModel;
import com.davivienda.factoraje.domain.entities.DocumentLogModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.service.DocumentLogService;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentLogServiceImpl implements DocumentLogService {

    private final DocumentLogRepository documentLogRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void createLogsForBatch(
            List<DocumentModel> documents, 
            DocumentStatusEnum status, 
            UUID performedBy, 
            UUID acceptanceAuditId) {

        if (documents == null || documents.isEmpty()) {
            return;
        }

        log.debug("Creating {} logs for the current document chunk", documents.size());

        UserModel userProxy = entityManager.getReference(UserModel.class, performedBy);
        AcceptanceAuditModel auditProxy = entityManager.getReference(AcceptanceAuditModel.class, acceptanceAuditId);

        List<DocumentLogModel> logsToSave = new ArrayList<>(documents.size());

        for (DocumentModel document : documents) {
            
            DocumentLogModel logEntry = DocumentLogModel.builder()
                    .document(document)
                    .status(status)
                    .user(userProxy)
                    .acceptanceAudit(auditProxy)
                    .build();

            logsToSave.add(logEntry);
        }

        documentLogRepository.saveAll(logsToSave);
        
        // IMPORTANTE: Se debe hacer flush aquí para que los INSERTs de los logs viajen a la BD 
        // ANTES de que el orquestador llame a entityManager.clear() y limpie la memoria.
        documentLogRepository.flush(); 
    }
}
