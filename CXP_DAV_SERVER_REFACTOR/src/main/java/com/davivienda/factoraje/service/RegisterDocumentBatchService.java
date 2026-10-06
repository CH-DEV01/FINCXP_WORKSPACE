package com.davivienda.factoraje.service;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTORequest;
import com.davivienda.factoraje.dto.upload_batch.BatchProcessResult;


public interface RegisterDocumentBatchService {

    public BatchProcessResult processFile(
        MultipartFile file,
        AcceptanceAuditDTORequest auditRequest,
        UUID payerId,
        UUID uploadedAndApprovedBy,
        boolean uploadedByAdmin);


}
