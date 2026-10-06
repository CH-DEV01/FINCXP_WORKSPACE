package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.davivienda.factoraje.dto.report.PdfReportData;
import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.infrastructure.exception.CreditLimitExceededException;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchErrorType;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchValidationError;
import com.davivienda.factoraje.infrastructure.reporting.PdfReportGenerator;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.lowagie.text.Element;

import lombok.RequiredArgsConstructor;

/** PDFs de respuesta de la carga de documentos: comprobante y reportes de rechazo. */
@Component
@RequiredArgsConstructor
public class UploadBatchReports {

    static final String REJECTION_TITLE = "Reporte de rechazo de carga";
    static final String CREDIT_LIMIT_REJECTION_TITLE = "Reporte de rechazo por límite de crédito";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final PdfReportGenerator pdfReportGenerator;

    public byte[] validationRejection(String originalFilename, String payerName, String uploadedBy,
            List<BatchValidationError> errors) {
        return rejection(REJECTION_TITLE, uploadMetadata(originalFilename, payerName, uploadedBy), errors);
    }

    public byte[] creditLimitRejection(String originalFilename, String payerName, String uploadedBy,
            CreditLimitExceededException exception) {
        Map<String, String> metadata = uploadMetadata(originalFilename, payerName, uploadedBy);
        metadata.put("Total del archivo", Money.format(exception.getBatchAmount()));
        metadata.put("Cupo disponible", Money.format(exception.getAvailableAmount()));
        return rejection(CREDIT_LIMIT_REJECTION_TITLE, metadata, List.of(new BatchValidationError(
                BatchValidationError.NO_ROW, "Monto", BatchErrorType.CREDIT_LIMIT_EXCEEDED, exception.getMessage())));
    }

    /** Rechazo que afecta al archivo completo y no a una fila en particular. */
    public byte[] generalRejection(String originalFilename, String payerName, String uploadedBy,
            BatchErrorType errorType, String message) {
        return rejection(REJECTION_TITLE, uploadMetadata(originalFilename, payerName, uploadedBy),
                List.of(BatchValidationError.general(errorType, message)));
    }

    /** El correlativo numera las inconsistencias, de modo que el último coincide con "Total de inconsistencias". */
    private byte[] rejection(String title, Map<String, String> metadata, List<BatchValidationError> errors) {
        List<BatchValidationError> sorted = errors.stream()
                .sorted(Comparator.comparingInt(BatchValidationError::rowIndex))
                .toList();
        List<List<String>> errorRows = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            BatchValidationError err = sorted.get(i);
            errorRows.add(List.of(
                    String.valueOf(i + 1),
                    err.rowIndex() != BatchValidationError.NO_ROW ? String.valueOf(err.rowIndex()) : "-",
                    err.columnName() != null ? err.columnName() : BatchValidationError.GENERAL_COLUMN,
                    err.errorType() != null ? err.errorType().getLabel() : "-",
                    err.errorMessage() != null ? err.errorMessage() : "-"));
        }
        metadata.put("Total de inconsistencias", String.valueOf(errors.size()));

        return pdfReportGenerator.generateReport(new PdfReportData(
                title,
                metadata,
                "Detalle de inconsistencias encontradas",
                null,
                List.of("Correlativo", "Fila", "Columna", "Tipo de error", "Detalle"),
                errorRows,
                new float[]{1.9f, 1.0f, 2.4f, 2.7f, 6.0f},
                new int[]{Element.ALIGN_CENTER, Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_LEFT,
                        Element.ALIGN_LEFT}
        ));
    }

    private static Map<String, String> uploadMetadata(String originalFilename, String payerName, String uploadedBy) {
        return metadata(
                "Archivo procesado", originalFilename,
                "Pagador", payerName,
                "Cargado por", uploadedBy);
    }

    /**
     * Comprobante de una carga exitosa. El correlativo numera los documentos registrados en el
     * orden del archivo, de modo que el último coincide con "Total procesados".
     *
     * @param supplierNames nombre registrado de cada proveedor por NIT; si falta, se usa el del archivo.
     */
    public byte[] receipt(String originalFilename, String payerName, String uploadedBy,
            List<InvoiceRecordDTO> invoices, Map<String, String> supplierNames) {
        List<List<String>> tableRows = new ArrayList<>(invoices.size());
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < invoices.size(); i++) {
            InvoiceRecordDTO inv = invoices.get(i);
            String supplierName = supplierNames.getOrDefault(inv.supplierNit(), inv.supplierName());
            tableRows.add(List.of(
                    String.valueOf(i + 1),
                    inv.documentNumber() != null ? inv.documentNumber() : "-",
                    inv.issueDate() != null ? inv.issueDate().format(DATE_FORMAT) : "-",
                    supplierName != null ? supplierName : "-",
                    Money.format(inv.nominalAmount())));
            if (inv.nominalAmount() != null) {
                total = total.add(inv.nominalAmount());
            }
        }

        return pdfReportGenerator.generateReport(new PdfReportData(
                "Comprobante de carga de facturas",
                metadata(
                        "Archivo procesado", originalFilename,
                        "Pagador", payerName,
                        "Cargado por", uploadedBy,
                        "Total procesados", String.valueOf(invoices.size())),
                "Detalle de documentos registrados",
                null,
                List.of("Correlativo", "N° de documento", "Fecha de emisión", "Nombre del proveedor", "Monto"),
                tableRows,
                new float[]{1.9f, 3.0f, 2.6f, 4.4f, 2.1f},
                new int[]{Element.ALIGN_CENTER, Element.ALIGN_LEFT, Element.ALIGN_CENTER, Element.ALIGN_LEFT,
                        Element.ALIGN_RIGHT},
                Money.format(total)
        ));
    }

    /** Metadatos del PDF en el orden indicado, con pares clave-valor. */
    private static Map<String, String> metadata(String... keyValues) {
        Map<String, String> metadata = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            metadata.put(keyValues[i], keyValues[i + 1] != null ? keyValues[i + 1] : "-");
        }
        return metadata;
    }
}
