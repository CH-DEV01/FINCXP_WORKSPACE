package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchValidationError;

public interface DocumentValidationService {
    
    /**
     * Evalúa las reglas de negocio financieras sobre un lote de facturas de un solo proveedor.
     * Acumula todos los errores encontrados para no interrumpir el flujo prematuramente.
     *
     * @param invoices facturas del mismo NIT parseadas del archivo
     * @param payerId pagador de la carga; sirve para contrastar el convenio ya registrado
     * @return errores de validación (vacía si todo es correcto)
     */
    List<BatchValidationError> validateBatch(List<InvoiceRecordDTO> invoices, UUID payerId);

    /** Duplicados de código de generación, número de control y sello en todo el archivo. */
    List<BatchValidationError> validateDteDuplicatesInFile(List<InvoiceRecordDTO> invoices);

    /**
     * Nombres de proveedores nuevos que chocan entre sí dentro del mismo archivo
     * (dos NIT distintos que aún no existen y traen el mismo nombre).
     */
    List<BatchValidationError> validateNewSupplierNamesInFile(List<InvoiceRecordDTO> invoices);
}
