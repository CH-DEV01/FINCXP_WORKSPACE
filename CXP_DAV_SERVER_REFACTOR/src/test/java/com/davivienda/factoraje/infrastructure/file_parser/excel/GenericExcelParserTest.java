package com.davivienda.factoraje.infrastructure.file_parser.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.infrastructure.exception.InvalidFileException;

class GenericExcelParserTest {

    private final GenericExcelParser parser = new GenericExcelParser();

    @Test
    void rejectsDuplicateHeadersIgnoringCase() throws Exception {
        byte[] content = workbook("Monto", "NIT", "monto");

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(content), List.of("Monto", "NIT"), 10,
                (accessor, rowIndex) -> "ok"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("más de una vez")
                .hasMessageContaining("monto");
    }

    @Test
    void stillRequiresExactHeaderNamesIncludingAccentsAndInnerSpaces() throws Exception {
        byte[] content = workbook("Fecha Emision", "Monto");

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(content), List.of("Fecha Emisión", "Monto"), 10,
                (accessor, rowIndex) -> "ok"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("falta la columna obligatoria")
                .hasMessageContaining("Fecha Emisión");
    }

    @Test
    void acceptsUniqueHeadersMatchingTheRequiredList() throws Exception {
        byte[] content = workbookWithData("Monto", "NIT");

        ExcelParseResult<String> result = parser.parse(new ByteArrayInputStream(content), List.of("Monto", "NIT"), 10,
                (accessor, rowIndex) -> "ok");

        assertThat(result.successfulRecords()).containsExactly("ok");
        assertThat(result.parsingErrors()).isEmpty();
    }

    @Test
    void rejectsWhenDocumentsSheetIsMissing() throws Exception {
        byte[] content;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Otra").createRow(0).createCell(0).setCellValue("Monto");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            content = out.toByteArray();
        }

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(content), List.of("Monto"), 10,
                (accessor, rowIndex) -> "ok"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessageContaining("no se encontró la hoja")
                .hasMessageContaining(GenericExcelParser.DOCUMENTS_SHEET_NAME);
    }

    @Test
    void ignoresOtherSheetsEvenIfTheyAreFirst() throws Exception {
        byte[] content;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Portada").createRow(0).createCell(0).setCellValue("Ignorar");
            Sheet documents = workbook.createSheet(GenericExcelParser.DOCUMENTS_SHEET_NAME);
            Row header = documents.createRow(0);
            header.createCell(0).setCellValue("Monto");
            header.createCell(1).setCellValue("NIT");
            documents.createRow(1).createCell(0).setCellValue(1);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            content = out.toByteArray();
        }

        ExcelParseResult<String> result = parser.parse(new ByteArrayInputStream(content), List.of("Monto", "NIT"), 10,
                (accessor, rowIndex) -> "ok");

        assertThat(result.successfulRecords()).containsExactly("ok");
    }

    private static byte[] workbook(String... headers) throws Exception {
        return workbook(false, headers);
    }

    private static byte[] workbookWithData(String... headers) throws Exception {
        return workbook(true, headers);
    }

    private static byte[] workbook(boolean withDataRow, String... headers) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(GenericExcelParser.DOCUMENTS_SHEET_NAME);
            Row row = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(headers[i]);
            }
            if (withDataRow) {
                sheet.createRow(1).createCell(0).setCellValue(1);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
