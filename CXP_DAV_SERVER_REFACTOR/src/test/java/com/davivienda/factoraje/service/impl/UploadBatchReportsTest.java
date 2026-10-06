package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.infrastructure.exception.CreditLimitExceededException;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchErrorType;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchValidationError;
import com.davivienda.factoraje.infrastructure.reporting.PdfReportGenerator;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

class UploadBatchReportsTest {

    private final UploadBatchReports reports = new UploadBatchReports(new PdfReportGenerator());

    @Test
    void receiptNumbersDocumentsUpToTheProcessedTotalAndShowsTheGrandTotal() throws Exception {
        List<InvoiceRecordDTO> invoices = List.of(
                invoice(7, "FAC-001", "06140101011011", "Nombre En Archivo", "1200.50"),
                invoice(9, "FAC-002", "06140202021022", "Servicios Cuscatlan", "300.25"),
                invoice(12, "FAC-003", "06140101011011", "Nombre En Archivo", "99.25"));

        String text = text(reports.receipt("carga.xlsx", "AEROMAN S.A. DE C.V", "Ana Operadora", invoices,
                Map.of("06140101011011", "Distribuidora Registrada")));

        assertThat(text)
                .contains("Archivo procesado: carga.xlsx", "Pagador: AEROMAN S.A. DE C.V",
                        "Cargado por: Ana Operadora", "Total procesados: 3")
                .contains("Correlativo", "N° de documento", "Fecha de emisión", "Nombre del proveedor", "Monto")
                .contains("Distribuidora Registrada", "Servicios Cuscatlan", "04/08/2026")
                .contains("Total", "$1,600.00")
                .doesNotContain("ID de lote", "Nombre En Archivo", "06140101011011", "Fila");
        assertThat(text.lines().map(String::strip).filter(line -> line.startsWith("3 FAC-003"))).hasSize(1);
        assertThat(text.lines().map(String::strip).filter(line -> line.startsWith("12 "))).isEmpty();
    }

    @Test
    void validationRejectionShowsErrorTypeAndUploaderAndNumbersInconsistencies() throws Exception {
        List<BatchValidationError> errors = List.of(
                new BatchValidationError(5, "Monto", BatchErrorType.VALUE_NOT_ALLOWED,
                        "El monto nominal de la factura debe ser mayor a cero."),
                new BatchValidationError(2, "Fecha de emision", BatchErrorType.INVALID_DATE,
                        "La fecha de emisión no puede ser futura."));

        String text = text(reports.validationRejection("carga.xlsx", "AEROMAN S.A. DE C.V", "Ana Operadora", errors));

        assertThat(text)
                .contains(UploadBatchReports.REJECTION_TITLE, "Archivo procesado: carga.xlsx",
                        "Pagador: AEROMAN S.A. DE C.V", "Cargado por: Ana Operadora", "Total de inconsistencias: 2")
                .contains("Correlativo", "Fila", "Columna", "Tipo de error", "Detalle")
                .contains("Fecha inválida", "Valor no permitido");
        assertThat(rowsStartingWith(text, "1 2 Fecha de emision")).hasSize(1);
        assertThat(rowsStartingWith(text, "2 5 Monto")).hasSize(1);
    }

    @Test
    void creditLimitRejectionUsesTheSameTableWithTheAmounts() throws Exception {
        String text = text(reports.creditLimitRejection("carga.xlsx", "AEROMAN S.A. DE C.V", "Ana Operadora",
                new CreditLimitExceededException(new BigDecimal("13000.00"), new BigDecimal("12000.00"))));

        assertThat(text)
                .contains(UploadBatchReports.CREDIT_LIMIT_REJECTION_TITLE, "Cargado por: Ana Operadora",
                        "Total del archivo: $13,000.00", "Cupo disponible: $12,000.00", "Total de inconsistencias: 1")
                .contains("Correlativo", "Tipo de error");
        assertThat(rowsStartingWith(text, "1 - Monto Límite de crédito")).hasSize(1);
    }

    @Test
    void generalRejectionHasNoRowAndUsesTheGeneralColumn() throws Exception {
        String text = text(reports.generalRejection("carga.xlsx", "AEROMAN S.A. DE C.V", null,
                BatchErrorType.CONCURRENT_UPLOAD, "Otra carga registró al mismo tiempo los documentos."));

        assertThat(text).contains("Cargado por: -", "Conflicto de carga");
        assertThat(rowsStartingWith(text, "1 - General")).hasSize(1);
    }

    private static List<String> rowsStartingWith(String text, String prefix) {
        return text.lines().map(String::strip).filter(line -> line.startsWith(prefix)).toList();
    }

    private static InvoiceRecordDTO invoice(int row, String number, String nit, String name, String amount) {
        return new InvoiceRecordDTO(row, LocalDate.of(2026, 8, 4), new BigDecimal(amount), number, null, null, null,
                "PAPER", "FCI", nit, name, "100200300400", "P30", "T_PLUS_1");
    }

    private static String text(byte[] pdf) throws Exception {
        try (PdfReader reader = new PdfReader(pdf)) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(extractor.getTextFromPage(page)).append('\n');
            }
            return text.toString();
        }
    }
}
