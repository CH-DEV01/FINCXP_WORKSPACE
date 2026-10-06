package com.davivienda.factoraje.infrastructure.file_parser.excel;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import com.davivienda.factoraje.infrastructure.util.Money;

/**
 * Las fórmulas no se evalúan: se usa el último valor que Excel guardó en la celda. Un archivo
 * generado por un programa que no guarda ese valor se lee como celda vacía o en cero.
 */
public class ExcelRowAccessor {

    private static final MathContext EXCEL_PRECISION = new MathContext(15, RoundingMode.HALF_UP);

    private final Row row;
    private final Map<String, Integer> headerIndexMap;
    private final DataFormatter formatter;

    public ExcelRowAccessor(Row row, Map<String, Integer> headerIndexMap, DataFormatter formatter) {
        this.row = row;
        this.headerIndexMap = headerIndexMap;
        this.formatter = formatter;
    }

    public String getString(String columnName) {
        Integer cellIndex = headerIndexMap.get(columnName.toLowerCase());
        if (cellIndex == null) return null;
        
        Cell cell = row.getCell(cellIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        
        // DataFormatter es seguro para textos, extrae exactamente lo que el usuario ve en pantalla
        String value = formatter.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }

    /**
     * Para identificadores (NIT, DUI, cuentas): en una celda numérica Excel pierde los
     * ceros a la izquierda y convierte números largos a notación científica, así que se exige texto.
     */
    public String getTextOnly(String columnName) {
        Integer cellIndex = headerIndexMap.get(columnName.toLowerCase());
        if (cellIndex == null) return null;

        Cell cell = row.getCell(cellIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;

        if (isNumeric(cell)) {
            throw new ExcelCellFormatException(columnName,
                "La celda es numérica. Aplique el formato de celda 'Texto' en Excel para conservar los ceros a la izquierda.");
        }

        return getString(columnName);
    }

    /**
     * Mapeo estricto para valores financieros: vacío devuelve {@code null} y no se redondea, se
     * rechaza la celda si trae más de 2 decimales. Rechaza cualquier celda que no esté formateada
     * nativamente como Número o Contabilidad.
     */
    public BigDecimal getBigDecimal(String columnName) {
        Integer cellIndex = headerIndexMap.get(columnName.toLowerCase());
        if (cellIndex == null) return null;

        Cell cell = row.getCell(cellIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;

        // VALIDACIÓN ESTRICTA: Solo acepta tipo NUMERIC (o fórmulas que devuelvan NUMERIC)
        if (isNumeric(cell)) {
            // Excel trabaja con 15 dígitos significativos: así 100.1 guardado como
            // 100.09999999999999 se lee como 100.1 y no como un monto con 14 decimales.
            BigDecimal amount = BigDecimal.valueOf(cell.getNumericCellValue())
                    .round(EXCEL_PRECISION)
                    .stripTrailingZeros();
            if (Money.hasMoreThanTwoDecimals(amount)) {
                throw new ExcelCellFormatException(columnName, String.format(
                        "El monto admite como máximo 2 decimales y la celda contiene %s. "
                                + "Corrija el valor en el archivo; el sistema no redondea montos.",
                        amount.toPlainString()));
            }
            return amount.setScale(Money.SCALE);
        }

        // Si llega aquí, el usuario intentó meter texto, símbolos manuales o un formato general sucio
        throw new ExcelCellFormatException(columnName,
            "La celda no tiene el formato correcto. Se requiere que aplique el formato de celda 'Número' o 'Contabilidad' en Excel.");
    }

    /**
     * Mapeo estricto para fechas.
     * Rechaza cualquier celda que no esté formateada nativamente como Fecha en Excel.
     */
    public LocalDate getLocalDate(String columnName) {
        Integer cellIndex = headerIndexMap.get(columnName.toLowerCase());
        if (cellIndex == null) return null;

        Cell cell = row.getCell(cellIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;

        // VALIDACIÓN ESTRICTA: Solo acepta NUMERIC + Formato de Fecha de Excel
        if (isNumeric(cell) && DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }

        // Si llega aquí, el usuario digitó la fecha como texto plano (Ej. "15/08/2023" en una celda general)
        throw new ExcelCellFormatException(columnName,
            "La celda no tiene el formato correcto. Se requiere que aplique el formato de celda 'Fecha' en Excel y no texto plano.");
    }

    private static boolean isNumeric(Cell cell) {
        return cell.getCellType() == CellType.NUMERIC
                || (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.NUMERIC);
    }
}
