package com.davivienda.factoraje.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTORequest;
import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;

public interface BatchPersistenceService {

    /**
     * Persiste la carga completa en una sola transacción: si se rechaza (límite de crédito,
     * duplicado concurrente), no quedan proveedores, cuentas, usuarios ni convenios creados.
     * Devuelve el ID de la carga registrada.
     */
    UUID persistBatch(
            EntityModel payer,
            Map<String, List<InvoiceRecordDTO>> invoicesBySupplier,
            String originalFilename,
            List<InvoiceRecordDTO> parsedInvoices,
            UUID uploadedAndApprovedBy,
            AcceptanceAuditDTORequest auditRequest);
}
