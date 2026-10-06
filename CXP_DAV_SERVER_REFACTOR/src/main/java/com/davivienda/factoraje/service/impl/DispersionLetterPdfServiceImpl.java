package com.davivienda.factoraje.service.impl;

import static com.davivienda.factoraje.service.impl.LetterPdfSupport.AGREEMENT_NAME;
import static com.davivienda.factoraje.service.impl.LetterPdfSupport.BANK_NAME;
import static com.davivienda.factoraje.service.impl.LetterPdfSupport.FIRST_COLUMN_FILL;
import static com.davivienda.factoraje.service.impl.LetterPdfSupport.GRID;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.dto.report.DispersionLetterData;
import com.davivienda.factoraje.service.DispersionLetterPdfService;
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

import lombok.extern.slf4j.Slf4j;

/** Carta "Solicitud de dispersión de pagos": una fila por proveedor y fecha de vencimiento. */
@Slf4j
@Service
public class DispersionLetterPdfServiceImpl implements DispersionLetterPdfService {

    private static final String[] HEADERS = {
            "Fecha de Dispersión", "Nombre Cuenta", "Cuenta a abonar", "Registros", "Fecha de Vencimiento",
            "Monto de Factura"
    };
    private static final float[] WIDTHS = {1.3f, 3.0f, 1.8f, 1.0f, 1.3f, 1.6f};
    private static final int AMOUNT_COLUMN = 5;
    private static final int ACCOUNT_COLUMN = 2;
    private static final int RECORDS_COLUMN = 3;

    private final LetterPdfSupport letter = new LetterPdfSupport();
    private final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, Color.WHITE);
    private final Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 6.5f);

    @Override
    public byte[] generate(DispersionLetterData data) {
        Document document = new Document(PageSize.LETTER, 54, 54, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(letter.header("Solicitud de dispersión de pagos -"));
            document.add(letter.spacer());

            Paragraph place = letter.rightAligned(letter.text("San Salvador, " + data.dispersionDate()));
            place.setSpacingAfter(14);
            document.add(place);

            document.add(letter.paragraph(letter.text("Estimados,")));
            document.add(letter.paragraph(letter.text(BANK_NAME)));
            document.add(letter.paragraph(letter.text("Presente")));
            document.add(letter.spacer());

            document.add(letter.paragraph(
                    letter.text("Yo, "), letter.bold(data.signerName()),
                    letter.text(" con número de DUI "), letter.bold(data.signerDui()),
                    letter.text(" usuario autorizado en el "),
                    letter.bold(AGREEMENT_NAME),
                    letter.text(" celebrado entre " + BANK_NAME + ", S.A. y "),
                    letter.bold(data.companyName()),
                    letter.text(", por este medio, solicito que se realice la dispersión de todas las facturas que "
                            + "no fueron anticipadas por los proveedores, a través del cargo a la cuenta "),
                    letter.bold(data.payerAccountNumber()),
                    letter.text(" y que se abonen de acuerdo al siguiente detalle:")));
            document.add(letter.spacer());

            document.add(detailTable(data));
            document.add(letter.spacer());

            document.add(letter.releaseOfLiability(data.companyName()));
            document.add(letter.spacer());

            document.add(letter.paragraph(letter.text("Atentamente,")));

        } catch (Exception e) {
            log.error("Error al generar la carta de solicitud de dispersión del lote {}", data.batchNumber(), e);
            throw new RuntimeException("Error interno al generar el documento PDF", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private PdfPTable detailTable(DispersionLetterData data) throws Exception {
        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setWidths(WIDTHS);
        table.setHeaderRows(1);

        for (String header : HEADERS) {
            table.addCell(letter.tableHeaderCell(header, headerFont));
        }

        for (DispersionLetterData.Row row : data.rows()) {
            String[] values = {
                    row.dispersionDate(), row.accountName(), row.accountNumber(), row.recordCount(), row.dueDate(),
                    row.invoiceAmount()
            };
            for (int i = 0; i < values.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(values[i] != null ? values[i] : "", cellFont));
                cell.setPadding(3);
                cell.setBorderColor(GRID);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                if (i == 0) {
                    cell.setBackgroundColor(FIRST_COLUMN_FILL);
                }
                if (i == AMOUNT_COLUMN || i == ACCOUNT_COLUMN) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                } else if (i == RECORDS_COLUMN) {
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                }
                table.addCell(cell);
            }
        }
        return table;
    }
}
