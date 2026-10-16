package com.davivienda.factoraje.infrastructure.util;

/**
 * Normaliza números de versión de términos para que "1.0", "1.00" y "01.0"
 * se traten como la misma versión (VAL-04).
 */
public final class VersionNumbers {

    private VersionNumbers() {
    }

    /** Quita ceros a la izquierda de cada segmento numérico. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        String[] parts = trimmed.split("\\.", -1);
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                normalized.append('.');
            }
            normalized.append(stripLeadingZeros(parts[i]));
        }
        return normalized.toString();
    }

    private static String stripLeadingZeros(String part) {
        if (part.isEmpty()) {
            return "0";
        }
        int index = 0;
        while (index < part.length() - 1 && part.charAt(index) == '0') {
            index++;
        }
        return part.substring(index);
    }
}
