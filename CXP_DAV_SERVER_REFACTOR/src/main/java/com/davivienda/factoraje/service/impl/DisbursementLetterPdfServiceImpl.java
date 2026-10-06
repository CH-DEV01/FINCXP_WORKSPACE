package com.davivienda.factoraje.service.impl;

import static com.davivienda.factoraje.service.impl.LetterPdfSupport.AGREEMENT_NAME;
import static com.davivienda.factoraje.service.impl.LetterPdfSupport.BANK_NAME;
import static com.davivienda.factoraje.service.impl.LetterPdfSupport.FIRST_COLUMN_FILL;
import static com.davivienda.factoraje.service.impl.LetterPdfSupport.GRID;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.dto.report.DisbursementLetterData;
import com.davivienda.factoraje.service.DisbursementLetterPdfService;
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

@Slf4j
@Service
public class DisbursementLetterPdfServiceImpl implements DisbursementLetterPdfService {

    private static final String[] HEADERS = {
            "Fecha de Solicitud de financiamiento", "Días de financiamiento", "Fecha de Vencimiento",
            "Monto total de la factura", "Intereses", "Monto a desembolsar*", "Comisión (IVA incluido)",
            "Monto a abonar**", "Cuenta a abonar", "Nombre Cuenta"
    };
    private static final float[] WIDTHS = {1.55f, 1.65f, 1.45f, 1.6f, 1.15f, 1.6f, 1.5f, 1.5f, 1.8f, 1.9f};
    private static final int FIRST_AMOUNT_COLUMN = 3;
    private static final int LAST_AMOUNT_COLUMN = 7;
    private static final String INFORMATIVE_NOTE = "* Monto a desembolsar: monto de la factura menos intereses.\n"
            + "** Monto a abonar: monto de la factura menos intereses y comisión (IVA incluido).";

    private final LetterPdfSupport letter = new LetterPdfSupport();
    private final Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6f, Color.WHITE);
    private final Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 6.5f);
    private final Font noteFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7);

    @Override
    public byte[] generate(DisbursementLetterData data) {
        Document document = new Document(PageSize.LETTER, 54, 54, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(letter.header("Solicitud de desembolso -"));
            document.add(letter.spacer());

            Paragraph place = letter.rightAligned(letter.text("San Salvador, " + data.requestDate()));
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
                    letter.text(", por este medio, solicito que se realice un desembolso del Cupo de Crédito con "
                            + "destino Adelanto de Facturas marcado con la referencia: "),
                    letter.bold(data.creditFacilityReference()),
                    letter.text(" por un monto de "), letter.bold(data.totalAmount()),
                    letter.text(" para que sea dispersado a mis proveedores de acuerdo al monto y número de cuenta "
                            + "que se detalla en el presente documento:")));
            document.add(letter.spacer());

            document.add(detailTable(data));
            Paragraph note = new Paragraph(INFORMATIVE_NOTE, noteFont);
            note.setSpacingBefore(4);
            document.add(note);
            document.add(letter.spacer());

            document.add(letter.releaseOfLiability(data.companyName()));
            document.add(letter.spacer());

            document.add(letter.paragraph(letter.text("Atentamente,")));

        } catch (Exception e) {
            log.error("Error al generar la carta de solicitud de desembolso del lote {}", data.batchNumber(), e);
            throw new RuntimeException("Error interno al generar el documento PDF", e);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }

    private PdfPTable detailTable(DisbursementLetterData data) throws Exception {
        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setWidths(WIDTHS);
        table.setHeaderRows(1);

        for (String header : HEADERS) {
            table.addCell(letter.tableHeaderCell(header, headerFont));
        }

        for (DisbursementLetterData.Row row : data.rows()) {
            String[] values = {
                    row.requestDate(), row.financingDays(), row.dueDate(), row.invoiceAmount(), row.interest(),
                    row.amountToDisburse(), row.commissionWithIva(), row.amountToCredit(), row.accountNumber(),
                    row.accountName()
            };
            for (int i = 0; i < values.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(values[i] != null ? values[i] : "", cellFont));
                cell.setPadding(3);
                cell.setBorderColor(GRID);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                if (i == 0) {
                    cell.setBackgroundColor(FIRST_COLUMN_FILL);
                }
                if (i >= FIRST_AMOUNT_COLUMN && i <= LAST_AMOUNT_COLUMN) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                } else if (i < FIRST_AMOUNT_COLUMN) {
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                }
                table.addCell(cell);
            }
        }
        return table;
    }
}
