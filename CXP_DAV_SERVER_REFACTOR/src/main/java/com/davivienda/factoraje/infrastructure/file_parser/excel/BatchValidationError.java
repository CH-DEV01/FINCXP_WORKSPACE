package com.davivienda.factoraje.infrastructure.file_parser.excel;

/**
 * @param rowIndex   fila del Excel; {@link #NO_ROW} cuando el error es del archivo completo.
 * @param columnName columna afectada o {@link #GENERAL_COLUMN}.
 */
public record BatchValidationError(
    int rowIndex,
    String columnName,
    BatchErrorType errorType,
    String errorMessage
) {
    public static final int NO_ROW = 0;
    public static final String GENERAL_COLUMN = "General";

    public static BatchValidationError general(BatchErrorType errorType, String errorMessage) {
        return new BatchValidationError(NO_ROW, GENERAL_COLUMN, errorType, errorMessage);
    }
}
