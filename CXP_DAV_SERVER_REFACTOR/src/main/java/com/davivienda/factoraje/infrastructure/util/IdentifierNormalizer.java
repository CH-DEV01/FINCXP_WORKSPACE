package com.davivienda.factoraje.infrastructure.util;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Formato único con el que se guardan y buscan los identificadores: NIT, DUI y
 * cuentas sin guiones ni espacios; correo solo con trim (se conserva mayúsculas y guiones);
 * campos del DTE en mayúsculas; número de factura en papel en mayúsculas sin espacios ni
 * guiones. Solo se quitan separadores en NIT/DUI/cuentas: si queda algo que no sea dígito,
 * la validación lo rechaza.
 */
public final class IdentifierNormalizer {

    private IdentifierNormalizer() {
    }

    public static String withoutSeparators(String value) {
        if (value == null) return null;
        String normalized = value.replaceAll("[\\s\\u00A0\\-]", "");
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Número de documento PAPER: mayúsculas y sin espacios ni guiones, para que
     * "F-001", "f-001" y "F 001" se traten como la misma factura.
     */
    public static String normalizePaperDocumentNumber(String value) {
        String normalized = withoutSeparators(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    /** Quita espacios al inicio y al final; no cambia mayúsculas ni el resto del texto. */
    public static String trimmed(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static String lowerCase(String value) {
        String normalized = trimmed(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    public static String upperCase(String value) {
        String normalized = trimmed(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
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

    public static String requireNiu(String value) {
        return requireMatch(withoutSeparators(value), NIU_PATTERN, "El NIU es obligatorio.",
                "El NIU solo puede contener números (máximo 25).");
    }

    public static String requireAccountNumber(String value) {
        return requireMatch(withoutSeparators(value), ACCOUNT_PATTERN, "La cuenta bancaria es obligatoria.",
                "La cuenta bancaria solo puede contener números (máximo 50).");
    }

    public static String requireEmail(String value) {
        return requireMatch(trimmed(value), EMAIL_PATTERN, "El correo electrónico es obligatorio.",
                "El correo electrónico no tiene un formato válido.");
    }

    private static final Pattern NIT_PATTERN = Pattern.compile("^\\d{14}$");
    private static final Pattern DUI_PATTERN = Pattern.compile("^\\d{9}$");
    private static final Pattern NIU_PATTERN = Pattern.compile("^\\d{1,25}$");
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
