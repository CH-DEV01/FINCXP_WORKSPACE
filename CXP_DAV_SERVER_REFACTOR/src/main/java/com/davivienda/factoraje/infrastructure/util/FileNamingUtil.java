package com.davivienda.factoraje.infrastructure.util;

public class FileNamingUtil {

    private FileNamingUtil() {
    }

    /**
     * Nombra el archivo del lote usando el identificador exacto del batch.
     * Ejemplo: UPL-20260917-A8B9C1.xlsx
     */
    public static String generateBatchFileName(String batchNumber, String originalFilename) {

        // Extraer la extensión de forma segura
        String extension = "";
        if (originalFilename != null) {
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            }
        }

        return batchNumber + extension;
    }
}
