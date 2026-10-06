package com.davivienda.factoraje.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvoiceTypeEnum {
    CCF("03", "Comprobante de Crédito Fiscal"),
    FCI("01", "Factura de consumidor final");

    /** Tipo de DTE del Ministerio de Hacienda; es el segundo segmento del número de control. */
    private final String dteCode;
    private final String description;

    public static InvoiceTypeEnum fromString(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return InvoiceTypeEnum.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static InvoiceTypeEnum fromDteCode(String dteCode) {
        for (InvoiceTypeEnum type : values()) {
            if (type.dteCode.equals(dteCode)) return type;
        }
        return null;
    }
}
