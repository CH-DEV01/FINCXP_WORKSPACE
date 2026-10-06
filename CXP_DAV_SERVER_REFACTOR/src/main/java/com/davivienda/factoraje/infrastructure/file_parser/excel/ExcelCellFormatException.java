package com.davivienda.factoraje.infrastructure.file_parser.excel;

import lombok.Getter;

@Getter
public class ExcelCellFormatException extends IllegalArgumentException {

    private final String columnName;

    public ExcelCellFormatException(String columnName, String message) {
        super(message);
        this.columnName = columnName;
    }
}
