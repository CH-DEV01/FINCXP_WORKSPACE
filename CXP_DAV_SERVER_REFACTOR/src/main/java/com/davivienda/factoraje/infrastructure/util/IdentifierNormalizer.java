package com.davivienda.factoraje.infrastructure.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Formato único con el que se guardan y buscan los identificadores: NIT, DUI y
 * cuentas sin guiones ni espacios; correo en minúsculas; campos del DTE en mayúsculas.
 * Solo se quitan separadores: si queda algo que no sea dígito, la validación lo rechaza.
 */
public final class IdentifierNormalizer {

    private IdentifierNormalizer() {
    }

    public static String withoutSeparators(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\s\\u00A0\\-]", "");
        return normalized.isEmpty() ? null : normalized;
    }

    public static String lowerCase(String value) {
        if (value == null) return null;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public static String upperCase(String value) {
        if (value == null) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    // Normalizan y validan en un paso para los formularios; lanzan IllegalArgumentException (HTTP 422).

    public static String requireNit(String value) {
        return requireMatch(withoutSeparators(value), NIT_PATTERN, "El NIT es obligatorio.",
                "El NIT debe tener exactamente 14 dígitos.");
    }

    public static String requireDui(String value) {
        return requireMatch(withoutSeparators(value), DUI_PATTERN, "El DUI es obligatorio.",
                "El DUI debe tener exactamente 9 dígitos.");
    }

    public static String requireAccountNumber(String value) {
        return requireMatch(withoutSeparators(value), ACCOUNT_PATTERN, "La cuenta bancaria es obligatoria.",
                "La cuenta bancaria solo puede contener números (máximo 50).");
    }

    public static String requireEmail(String value) {
        return requireMatch(lowerCase(value), EMAIL_PATTERN, "El correo electrónico es obligatorio.",
                "El correo electrónico no tiene un formato válido.");
    }

    private static final Pattern NIT_PATTERN = Pattern.compile("^\\d{14}$");
    private static final Pattern DUI_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^\\d{1,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static String requireMatch(String value, Pattern pattern, String requiredMessage, String formatMessage) {
        if (value == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(formatMessage + " Valor recibido: " + value);
        }
        return value;
    }
}
