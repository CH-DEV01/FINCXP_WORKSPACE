import java.awt.Color;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Genera db/prod/manual_carga_documentos.pdf, el manual inicial de la plantilla de carga que el
 * ADMIN publica desde Recursos de carga. Si cambian las columnas o las validaciones de la carga,
 * actualice el contenido, vuelva a generarlo y publíquelo.
 *
 * Uso, desde CXP_DAV_SERVER_REFACTOR (OpenPDF ya está en ~/.m2 por la dependencia del proyecto):
 *   java -cp ~/.m2/repository/com/github/librepdf/openpdf/1.3.39/openpdf-1.3.39.jar db/prod/GenerarManualCarga.java
 */
public class GenerarManualCarga {

    private static final Path OUTPUT = Path.of("db/prod/manual_carga_documentos.pdf");
    private static final Color RED = new Color(200, 16, 46);
    private static final Color TEXT = new Color(55, 65, 81);
    private static final Color ROW_ALT = new Color(249, 250, 251);

    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, RED);
    private static final Font SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);
    private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, RED);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10, TEXT);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, TEXT);
    private static final Font CELL = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT);
    private static final Font CELL_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXT);
    private static final Font CELL_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

    private static final String[][] COLUMNS = {
            { "Fecha Emision", "Sí", "Fecha",
                    "Fecha de emisión del documento. No puede ser futura ni superar la antigüedad máxima que configura el banco." },
            { "Monto", "Sí", "Número o Contabilidad",
                    "Monto del documento, mayor a cero y con máximo 2 decimales. El sistema no redondea." },
            { "Numero Documento", "Sí, para documentos en papel", "Texto",
                    "Número del documento (máximo 255 caracteres)." },
            { "Codigo Generacion", "Sí, para documentos digitales", "Texto",
                    "Código de generación del DTE: 36 caracteres con el formato 8-4-4-4-12 (hexadecimal). Vacío en documentos en papel." },
            { "Sello Recepcion", "Sí, para documentos digitales", "Texto",
                    "Sello de recepción del DTE: 40 letras y números. Vacío en documentos en papel." },
            { "Numero Control", "Sí, para documentos digitales", "Texto",
                    "Número de control con el formato DTE-TT-XXXXXXXX-NNNNNNNNNNNNNNN. TT es 01 (factura de consumidor final) "
                            + "o 03 (comprobante de crédito fiscal) y debe coincidir con Tipo Factura. Vacío en documentos en papel." },
            { "Metodo Emision", "Sí", "Texto", "DIGITAL o PAPER." },
            { "Tipo Factura", "Sí", "Texto", "CCF (comprobante de crédito fiscal) o FCI (factura de consumidor final)." },
            { "NIT Proveedor", "Sí", "Texto", "NIT del proveedor: 14 dígitos, con o sin guiones." },
            { "Nombre Proveedor", "Sí", "Texto", "Nombre o razón social del proveedor (máximo 255 caracteres)." },
            { "Politica Pago", "Sí", "Texto", "Código de la política de pago, por ejemplo P30, P45, P60 o P90." },
            { "Dia Desembolso", "Sí", "Texto", "Código de la política de desembolso, por ejemplo T_PLUS_1 o ONLY_FRIDAYS." },
            { "Cuenta Bancaria", "Sí", "Texto", "Cuenta del proveedor, solo números (máximo 50)." },
    };

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUTPUT.getParent());
        Document document = new Document(PageSize.LETTER, 50, 50, 50, 50);
        try (OutputStream out = new FileOutputStream(OUTPUT.toFile())) {
            PdfWriter.getInstance(document, out);
            document.addTitle("Manual de uso de la plantilla de carga");
            document.addAuthor("Banco Davivienda");
            HeaderFooter footer = new HeaderFooter(new Phrase("Página ", SUBTITLE), true);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setBorder(Rectangle.NO_BORDER);
            document.setFooter(footer);
            document.open();

            document.add(new Paragraph("Manual de uso de la plantilla de carga", TITLE));
            Paragraph subtitle = new Paragraph("Financiamiento de Cuentas por Pagar", SUBTITLE);
            subtitle.setSpacingAfter(10);
            document.add(subtitle);

            heading(document, "Antes de empezar");
            List before = bullets();
            before.add(item(null, "Descargue la plantilla vigente desde la sección Recursos de la pantalla de carga."));
            before.add(item(null, "La primera fila de la primera hoja contiene los encabezados. No los cambie ni los mueva de fila; "
                    + "el orden de las columnas sí puede cambiar."));
            before.add(item(null, "Cada fila a partir de la segunda es un documento. Las filas vacías se ignoran."));
            before.add(item(null, "Un mismo archivo puede incluir documentos de varios proveedores."));
            before.add(item(null, "El archivo debe ser .xlsx o .xls y respetar el tamaño máximo y el número máximo de registros "
                    + "que configura el banco."));
            before.add(item(null, "Antes de digitar, aplique el formato de celda Texto a las columnas NIT Proveedor y "
                    + "Cuenta Bancaria para conservar los ceros a la izquierda."));
            document.add(before);

            heading(document, "Columnas");
            document.add(columnsTable());

            heading(document, "Reglas que se validan");
            List rules = bullets();
            rules.add(item("Cuenta del proveedor: ", "todas las filas de un proveedor deben traer la misma cuenta. Si el proveedor "
                    + "ya está registrado, debe coincidir con su cuenta registrada, y la cuenta no puede pertenecer a otra entidad."));
            rules.add(item("Documentos digitales: ", "el código de generación, el número de control y el sello de recepción no "
                    + "pueden repetirse dentro del archivo ni existir ya en el sistema."));
            rules.add(item("Documentos en papel: ", "el número de documento no puede repetirse para el mismo proveedor en el "
                    + "mismo año, ni dentro del archivo ni en el sistema."));
            rules.add(item("Fórmulas: ", "se toma el valor que Excel dejó guardado en la celda."));
            document.add(rules);

            heading(document, "Resultado de la carga");
            List result = bullets();
            result.add(item(null, "Si todo es válido, se registran los documentos y se descarga un comprobante en PDF."));
            result.add(item(null, "Si hay errores, la carga se rechaza completa y se descarga un reporte de inconsistencias en PDF "
                    + "con la fila, la columna y el motivo de cada error. No se guarda ningún documento hasta que se corrija "
                    + "el archivo y se vuelva a cargar."));
            result.add(item(null, "La carga también se rechaza si el total supera la línea de crédito disponible del pagador."));
            document.add(result);
            document.close();
        }
        System.out.println("Manual generado en " + OUTPUT.toAbsolutePath());
    }

    private static void heading(Document document, String text) throws Exception {
        Paragraph heading = new Paragraph(text, HEADING);
        heading.setSpacingBefore(14);
        heading.setSpacingAfter(6);
        document.add(heading);
    }

    private static List bullets() {
        List list = new List(List.UNORDERED, 12);
        list.setListSymbol(new Chunk("•", BODY));
        return list;
    }

    private static ListItem item(String boldPrefix, String text) {
        ListItem item = new ListItem();
        item.setLeading(14);
        item.setSpacingAfter(3);
        if (boldPrefix != null) {
            item.add(new Chunk(boldPrefix, BOLD));
        }
        item.add(new Chunk(text, BODY));
        return item;
    }

    private static PdfPTable columnsTable() {
        PdfPTable table = new PdfPTable(new float[] { 2.2f, 2.2f, 1.8f, 5.8f });
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String header : new String[] { "Columna", "Obligatoria", "Formato de celda", "Qué debe contener" }) {
            PdfPCell cell = cell(header, CELL_HEADER);
            cell.setBackgroundColor(RED);
            table.addCell(cell);
        }
        for (int row = 0; row < COLUMNS.length; row++) {
            for (int col = 0; col < COLUMNS[row].length; col++) {
                PdfPCell cell = cell(COLUMNS[row][col], col == 0 ? CELL_BOLD : CELL);
                if (row % 2 == 1) {
                    cell.setBackgroundColor(ROW_ALT);
                }
                table.addCell(cell);
            }
        }
        return table;
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorderColor(new Color(229, 231, 235));
        return cell;
    }
}
