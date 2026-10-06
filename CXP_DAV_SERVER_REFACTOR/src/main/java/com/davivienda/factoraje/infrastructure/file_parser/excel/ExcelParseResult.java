package com.davivienda.factoraje.infrastructure.file_parser.excel;

import java.util.List;

public record ExcelParseResult<T>(
    List<T> successfulRecords,
    List<BatchValidationError> parsingErrors
) {
    public boolean hasErrors() {
        return !parsingErrors.isEmpty();
    }
}