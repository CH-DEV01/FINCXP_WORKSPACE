package com.davivienda.factoraje.infrastructure.file_parser.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExcelRowAccessorTest {

    private XSSFWorkbook workbook;
    private Row row;
    private ExcelRowAccessor accessor;

    @BeforeEach
    void setUp() {
        workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        row = sheet.createRow(0);
        accessor = new ExcelRowAccessor(row, Map.of("numero de documento", 0), new DataFormatter());
    }

    @AfterEach
    void tearDown() throws Exception {
        workbook.close();
    }

    @Test
    void textOnlyKeepsLeadingZerosFromATextCell() {
        row.createCell(0, CellType.STRING).setCellValue("00123");

        assertThat(accessor.getTextOnly("Numero de documento")).isEqualTo("00123");
    }

    @Test
    void textOnlyRejectsANumericCellSoLeadingZerosAreNotLost() {
        row.createCell(0, CellType.NUMERIC).setCellValue(123);

        assertThatThrownBy(() -> accessor.getTextOnly("Numero de documento"))
                .isInstanceOf(ExcelCellFormatException.class)
                .hasMessageContaining("formato de celda 'Texto'");
    }
}
