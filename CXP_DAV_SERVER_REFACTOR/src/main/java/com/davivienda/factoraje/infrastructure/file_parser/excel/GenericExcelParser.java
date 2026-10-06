package com.davivienda.factoraje.infrastructure.file_parser.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import com.davivienda.factoraje.infrastructure.exception.InvalidFileException;
import com.github.pjfanning.xlsx.StreamingReader;

/**
 * Los .xlsx se leen en streaming (solo unas filas en memoria y los textos compartidos en un
 * archivo temporal); los .xls no tienen compresión y su tamaño ya lo acota el límite de carga.
 * Las fórmulas no se evalúan: se usa el valor que Excel dejó guardado en la celda.
 */
@Component
public class GenericExcelParser {

    private static final int ROW_CACHE_SIZE = 100;
    private static final int BUFFER_SIZE = 4096;
    public static final String UNREADABLE_FILE_MESSAGE =
            "El archivo no pudo ser leído. Verifique que sea un Excel válido.";

    static {
        // Límite global de POI (por defecto 4 GB por entrada del zip) contra zip bombs; el ratio
        // mínimo de compresión (1:100) y el número de entradas ya vienen limitados por defecto.
        ZipSecureFile.setMaxEntrySize(256L * 1024 * 1024);
    }

    /**
     * @param <T>             El tipo de Record a construir.
     * @param inputStream     El contenido del archivo (.xlsx o .xls).
     * @param requiredHeaders Nombres de las columnas que no pueden faltar en el Excel.
     * @param maxRows         Máximo de filas con datos; al superarlo se corta la lectura.
     * @param rowMapper       La expresión lambda que construirá el Record.
     * @return Un objeto Result que contiene registros exitosos y errores acumulados.
     * @throws InvalidFileException si el archivo no es un Excel, le faltan encabezados o supera {@code maxRows}.
     */
    public <T> ExcelParseResult<T> parse(InputStream inputStream, List<String> requiredHeaders, int maxRows,
            ExcelRowMapper<T> rowMapper) throws IOException {

        try (Workbook workbook = openWorkbook(inputStream)) {
            return parseFirstSheet(workbook, requiredHeaders, maxRows, rowMapper);
        }
    }

    private Workbook openWorkbook(InputStream inputStream) throws IOException {
        InputStream stream = FileMagic.prepareToCheckMagic(inputStream);
        FileMagic magic = FileMagic.valueOf(stream);
        if (magic != FileMagic.OOXML && magic != FileMagic.OLE2) {
            throw new InvalidFileException("El archivo no es un Excel válido (.xlsx o .xls).");
        }
        try {
            return magic == FileMagic.OOXML
                    ? StreamingReader.builder()
                            .rowCacheSize(ROW_CACHE_SIZE)
                            .bufferSize(BUFFER_SIZE)
                            .setUseSstTempFile(true)
                            .setReadComments(false)
                            .setReadHyperlinks(false)
                            .setReadShapes(false)
                            .setReadCoreProperties(false)
                            .open(stream)
                    : WorkbookFactory.create(stream);
        } catch (RuntimeException e) {
            throw new InvalidFileException(UNREADABLE_FILE_MESSAGE, e);
        }
    }

    /** El lector en streaming descomprime al avanzar, así que un archivo dañado falla aquí y no al abrirlo. */
    private static Row nextRowOrNull(Iterator<Row> rows) {
        try {
            return rows.hasNext() ? rows.next() : null;
        } catch (RuntimeException e) {
            throw new InvalidFileException(UNREADABLE_FILE_MESSAGE, e);
        }
    }

    private <T> ExcelParseResult<T> parseFirstSheet(Workbook workbook, List<String> requiredHeaders, int maxRows,
            ExcelRowMapper<T> rowMapper) {

        if (workbook.getNumberOfSheets() == 0) {
            throw new InvalidFileException("El archivo está vacío o la primera fila no contiene los encabezados.");
        }
        Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rows = sheet.iterator();

        DataFormatter formatter = new DataFormatter();
        formatter.setUseCachedValuesForFormulaCells(true);

        Row headerRow = nextRowOrNull(rows);
        if (headerRow == null || headerRow.getRowNum() != 0) {
            throw new InvalidFileException("El archivo está vacío o la primera fila no contiene los encabezados.");
        }

        Map<String, Integer> headerIndexMap = buildHeaderIndexMap(headerRow, formatter);
        validateHeaders(headerIndexMap, requiredHeaders);

        List<T> successfulRecords = new ArrayList<>();
        List<BatchValidationError> errors = new ArrayList<>();
        int dataRows = 0;

        Row row;
        while ((row = nextRowOrNull(rows)) != null) {
            if (isRowEmpty(row))
                continue;

            if (++dataRows > maxRows) {
                throw new InvalidFileException(String.format(
                        "El archivo supera el máximo permitido de %d registros por carga.", maxRows));
            }

            int excelRowNumber = row.getRowNum() + 1;
            ExcelRowAccessor accessor = new ExcelRowAccessor(row, headerIndexMap, formatter);

            try {
                T dto = rowMapper.mapRow(accessor, excelRowNumber);
                successfulRecords.add(dto);
            } catch (ExcelCellFormatException e) {
                errors.add(new BatchValidationError(excelRowNumber, e.getColumnName(),
                        BatchErrorType.INVALID_FORMAT, e.getMessage()));
            } catch (IllegalArgumentException e) {
                errors.add(new BatchValidationError(excelRowNumber, "N/A", BatchErrorType.INVALID_FORMAT, e.getMessage()));
            } catch (Exception e) {
                errors.add(new BatchValidationError(excelRowNumber, "N/A", BatchErrorType.INVALID_FORMAT,
                        "Error inesperado al procesar la fila: " + e.getMessage()));
            }
        }

        return new ExcelParseResult<>(successfulRecords, errors);
    }

    private Map<String, Integer> buildHeaderIndexMap(Row headerRow, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String headerName = formatter.formatCellValue(cell).trim().toLowerCase();
            if (!headerName.isBlank()) {
                map.put(headerName, cell.getColumnIndex());
            }
        }
        return map;
    }

    private void validateHeaders(Map<String, Integer> headerIndexMap, List<String> requiredHeaders) {
        if (requiredHeaders == null)
            return;

        for (String req : requiredHeaders) {
            if (!headerIndexMap.containsKey(req.toLowerCase())) {
                throw new InvalidFileException("Archivo rechazado: falta la columna obligatoria \"" + req + "\".");
            }
        }
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
