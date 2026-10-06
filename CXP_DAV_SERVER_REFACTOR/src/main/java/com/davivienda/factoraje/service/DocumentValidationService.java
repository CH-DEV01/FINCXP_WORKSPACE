package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchValidationError;

public interface DocumentValidationService {
    
    /**
     * Evalúa las reglas de negocio financieras sobre un lote de facturas extraídas del Excel.
     * Acumula todos los errores encontrados para no interrumpir el flujo prematuramente.
     *
     * @param invoices Lista de facturas parseadas del archivo.
     * @param masterAgreementId ID del convenio marco para verificar duplicidad.
     * @return Lista de errores de validación (vacía si todo es correcto).
     */
    List<BatchValidationError> validateBatch(List<InvoiceRecordDTO> invoices, UUID masterAgreementId);

    /** Duplicados de código de generación, número de control y sello en todo el archivo. */
    List<BatchValidationError> validateDteDuplicatesInFile(List<InvoiceRecordDTO> invoices);
}
