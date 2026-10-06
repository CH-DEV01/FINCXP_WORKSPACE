package com.davivienda.factoraje.dto.report;

import java.util.List;
import java.util.Map;

public record PdfReportData(
    String title,
    Map<String, String> summaryMetadata, // Pares Clave-Valor (Ej: "Total procesados": "150")
    String listTitle,                    // Título de la sección (Ej: "Detalle de Facturas")
    List<String> listItems,              // Para listas simples / unicolumna (Ej: Errores)
    List<String> tableHeaders,           // Encabezados de columnas (Ej: ["Fila", "N° Doc", "Monto"])
    List<List<String>> tableRows,        // Filas estructuradas para reportes multicolumna
    float[] columnWidths,                // Anchos relativos por columna (opcional)
    int[] columnAlignments,              // Element.ALIGN_* por columna (opcional)
    String tableTotal                    // Valor de la fila "Total" bajo la última columna (opcional)
) {
    // Constructor de conveniencia para mantener compatibilidad con reportes simples unicolumna
    public PdfReportData(String title, Map<String, String> summaryMetadata, String listTitle, List<String> listItems) {
        this(title, summaryMetadata, listTitle, listItems, null, null, null, null, null);
    }

    public PdfReportData(String title, Map<String, String> summaryMetadata, String listTitle, List<String> listItems,
                         List<String> tableHeaders, List<List<String>> tableRows) {
        this(title, summaryMetadata, listTitle, listItems, tableHeaders, tableRows, null, null, null);
    }

    public PdfReportData(String title, Map<String, String> summaryMetadata, String listTitle, List<String> listItems,
                         List<String> tableHeaders, List<List<String>> tableRows, float[] columnWidths,
                         int[] columnAlignments) {
        this(title, summaryMetadata, listTitle, listItems, tableHeaders, tableRows, columnWidths, columnAlignments,
                null);
    }
}
