package com.davivienda.factoraje.domain.enums;

public enum IssuanceMethodEnum {
    DIGITAL,
    PAPER;

    public static IssuanceMethodEnum fromString(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return IssuanceMethodEnum.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
