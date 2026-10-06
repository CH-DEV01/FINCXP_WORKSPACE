package com.davivienda.factoraje.infrastructure.reporting;

import com.davivienda.factoraje.dto.report.PdfReportData;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Component
public class PdfReportGenerator {

    public byte[] generateReport(PdfReportData reportData) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(220, 38, 38));
            Paragraph title = new Paragraph(reportData.title(), titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            title.setSpacingAfter(15);
            document.add(title);

            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            document.add(new Paragraph("Fecha de ejecución: " + date, normalFont));
            
            if (reportData.summaryMetadata() != null) {
                for (Map.Entry<String, String> entry : reportData.summaryMetadata().entrySet()) {
                    document.add(new Paragraph(entry.getKey() + ": " + entry.getValue(), normalFont));
                }
            }
            document.add(new Paragraph(" "));

            // Tabla multicolumna: detalle estructurado de facturas
            if (reportData.tableHeaders() != null && !reportData.tableHeaders().isEmpty() 
                    && reportData.tableRows() != null && !reportData.tableRows().isEmpty()) {
                
                if (reportData.listTitle() != null) {
                    Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
                    Paragraph sectionTitle = new Paragraph(reportData.listTitle(), sectionTitleFont);
                    sectionTitle.setSpacingAfter(10);
                    document.add(sectionTitle);
                }

                int numColumns = reportData.tableHeaders().size();
                PdfPTable table = new PdfPTable(numColumns);
                table.setWidthPercentage(100);

                float[] widths = reportData.columnWidths();
                int[] alignments = reportData.columnAlignments();
                if (widths != null && widths.length == numColumns) {
                    table.setWidths(widths);
                } else if (numColumns == 5) {
                    // Fila, Doc, Fecha, NIT, Monto
                    table.setWidths(new float[]{1.2f, 3.0f, 2.3f, 2.3f, 2.5f});
                }
                table.setHeaderRows(1);

                // Encabezados dinámicos
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
                for (String headerText : reportData.tableHeaders()) {
                    PdfPCell headerCell = new PdfPCell(new Phrase(headerText, headerFont));
                    headerCell.setBackgroundColor(new Color(220, 38, 38)); // Encabezado rojo
                    headerCell.setPadding(6);
                    headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(headerCell);
                }

                // Filas de la tabla
                Font itemFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                for (List<String> row : reportData.tableRows()) {
                    int colIndex = 0;
                    for (String cellValue : row) {
                        PdfPCell cell = new PdfPCell(new Phrase(cellValue != null ? cellValue : "-", itemFont));
                        cell.setPadding(5);
                        
                        if (alignments != null && colIndex < alignments.length) {
                            cell.setHorizontalAlignment(alignments[colIndex]);
                        } else if (colIndex == 0) {
                            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        } else if (colIndex == row.size() - 1) {
                            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        } else {
                            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        }
                        
                        table.addCell(cell);
                        colIndex++;
                    }
                }
                if (reportData.tableTotal() != null) {
                    Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.BLACK);
                    Color totalBackground = new Color(243, 244, 246);
                    PdfPCell labelCell = new PdfPCell(new Phrase("Total", totalFont));
                    labelCell.setColspan(numColumns - 1);
                    labelCell.setPadding(5);
                    labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    labelCell.setBackgroundColor(totalBackground);
                    table.addCell(labelCell);

                    PdfPCell totalCell = new PdfPCell(new Phrase(reportData.tableTotal(), totalFont));
                    totalCell.setPadding(5);
                    totalCell.setHorizontalAlignment(alignments != null && alignments.length == numColumns
                            ? alignments[numColumns - 1] : Element.ALIGN_RIGHT);
                    totalCell.setBackgroundColor(totalBackground);
                    table.addCell(totalCell);
                }
                document.add(table);

            // Tabla de una columna: listas simples o de errores
            } else if (reportData.listItems() != null && !reportData.listItems().isEmpty()) {
                
                if (reportData.listTitle() != null) {
                    Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(220, 38, 38));
                    Paragraph sectionTitle = new Paragraph(reportData.listTitle(), sectionTitleFont);
                    sectionTitle.setSpacingAfter(10);
                    document.add(sectionTitle);
                }

                PdfPTable table = new PdfPTable(1);
                table.setWidthPercentage(100);

                PdfPCell headerCell = new PdfPCell(new Phrase("Descripción", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
                headerCell.setBackgroundColor(Color.DARK_GRAY);
                headerCell.setPadding(6);
                table.addCell(headerCell);

                Font itemFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
                for (String item : reportData.listItems()) {
                    PdfPCell cell = new PdfPCell(new Phrase(item, itemFont));
                    cell.setPadding(5);
                    table.addCell(cell);
                }
                document.add(table);

            } else {
                Font successFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(16, 185, 129));
                document.add(new Paragraph("Operación completada sin observaciones.", successFont));
            }

            document.close();
            return baos.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el documento PDF", e);
        }
    }
}