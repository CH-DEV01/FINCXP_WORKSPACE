package com.davivienda.factoraje.infrastructure.file_parser.excel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Tipo de error mostrado en el reporte de rechazo de carga. */
@Getter
@RequiredArgsConstructor
public enum BatchErrorType {
    REQUIRED_FIELD("Campo obligatorio"),
    INVALID_FORMAT("Formato inválido"),
    MAX_LENGTH_EXCEEDED("Longitud excedida"),
    VALUE_NOT_ALLOWED("Valor no permitido"),
    INVALID_DATE("Fecha inválida"),
    DUPLICATE_IN_FILE("Duplicado en el archivo"),
    DOUBLE_FUNDING("Doble fondeo"),
    SUPPLIER_ACCOUNT("Proveedor/cuenta bancaria"),
    CREDIT_LIMIT_EXCEEDED("Límite de crédito excedido"),
    RESOURCE_UNAVAILABLE("Recurso no disponible"),
    CONCURRENT_UPLOAD("Conflicto de carga simultánea");

    private final String label;
}
