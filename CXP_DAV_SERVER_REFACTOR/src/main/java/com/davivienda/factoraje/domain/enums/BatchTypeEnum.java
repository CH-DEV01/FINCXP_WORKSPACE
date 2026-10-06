package com.davivienda.factoraje.domain.enums;

public enum BatchTypeEnum {

    UPLOAD("UPL"), // Lotes de carga (Documentos / DTEs)
    DISBURSEMENT("DSB"), // Lotes de confirmación de desembolsos
    DISPERSION("DSP"); // Lotes de dispersión de documentos no financiables

    private final String prefix;

    BatchTypeEnum(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
