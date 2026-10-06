package com.davivienda.factoraje.service.impl;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.springframework.core.io.ClassPathResource;

import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;

import lombok.extern.slf4j.Slf4j;

/** Encabezado, estilos y textos comunes de las cartas que el pagador dirige al banco. */
@Slf4j
final class LetterPdfSupport {

    static final String BANK_NAME = "Banco Davivienda Salvadoreño";
    static final String AGREEMENT_NAME = "Convenio De Servicio Bancario Para La Gestión Y Anticipo De Cuentas Por Pagar";

    static final Color BRAND_RED = new Color(0xE3, 0x06, 0x13);
    static final Color FIRST_COLUMN_FILL = new Color(0xFB, 0xDD, 0xDB);
    static final Color GRID = new Color(0xBF, 0xBF, 0xBF);

    private static final String LOGO_PATH = "reports/davivienda-logo.png";
    // Región de la casa como fracción del logo completo (x, y, ancho, alto), sin la palabra DAVIVIENDA.
    private static final double[] LOGO_HOUSE_REGION = {0.29, 0.085, 0.42, 0.62};
    private static final int LOGO_WIDTH_PX = 160;

    // Anchos en puntos del encabezado; el título crece con su texto para no partirse en dos líneas.
    private static final float MIN_TITLE_WIDTH = 231f;
    private static final float TITLE_PADDING = 8f;
    private static final float BANNER_WIDTH = 105f;
    private static final float LOGO_WIDTH = 37f;

    final Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 15, BRAND_RED);
    final Font bannerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLDOBLIQUE, 7.5f, Color.WHITE);
    final Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
    final Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

    private final byte[] logo = loadLogoHouse();

    /** Título en rojo, banda "Financiamiento de cuentas por pagar" y la casa de Davivienda, alineados a la derecha. */
    PdfPTable header(String title) throws Exception {
        float titleWidth = Math.max(MIN_TITLE_WIDTH,
                titleFont.getBaseFont().getWidthPoint(title, titleFont.getSize()) + TITLE_PADDING);
        PdfPTable header = new PdfPTable(logo != null ? 3 : 2);
        header.setTotalWidth(logo != null
                ? new float[]{titleWidth, BANNER_WIDTH, LOGO_WIDTH}
                : new float[]{titleWidth, BANNER_WIDTH});
        header.setLockedWidth(true);
        header.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell titleCell = new PdfPCell(new Phrase(title, titleFont));
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setPaddingRight(4);
        header.addCell(titleCell);

        PdfPCell banner = new PdfPCell(new Phrase("Financiamiento de\ncuentas por pagar", bannerFont));
        banner.setBorder(Rectangle.NO_BORDER);
        banner.setBackgroundColor(BRAND_RED);
        banner.setHorizontalAlignment(Element.ALIGN_CENTER);
        banner.setVerticalAlignment(Element.ALIGN_MIDDLE);
        banner.setPadding(3);
        header.addCell(banner);

        if (logo != null) {
            Image image = Image.getInstance(logo);
            image.scaleToFit(22, 18);
            PdfPCell logoCell = new PdfPCell(image, false);
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setPaddingLeft(3);
            header.addCell(logoCell);
        }
        return header;
    }

    /** Declaración final con la que el pagador libera al banco de disputas con sus proveedores. */
    Paragraph releaseOfLiability(String companyName) {
        return paragraph(
                text("Reconozco, declaro y acepto de manera expresa e irrevocable que la información detallada "
                        + "en el presente documento ha sido aprobada por mi persona de acuerdo a los Términos y "
                        + "Condiciones aplicables al Servicio Bancario para la Gestión Pago y Anticipo de Pago a "
                        + "Proveedores, los cuales se encuentran establecidos al cargar el archivo de Cuentas por "
                        + "Pagar al Portal para tal efecto, por lo que libero a " + BANK_NAME + ", S.A., de toda "
                        + "responsabilidad por cualquier disputa o reclamo que pueda surgir entre "),
                bold(companyName),
                text(" y sus proveedores en relación con los desembolsos realizados bajo esta autorización."));
    }

    PdfPCell tableHeaderCell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setBackgroundColor(BRAND_RED);
        cell.setBorderColor(Color.WHITE);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPadding(3);
        return cell;
    }

    Paragraph paragraph(Chunk... chunks) {
        Paragraph paragraph = new Paragraph();
        paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
        paragraph.setLeading(13);
        for (Chunk chunk : chunks) {
            paragraph.add(chunk);
        }
        return paragraph;
    }

    Paragraph rightAligned(Chunk chunk) {
        Paragraph paragraph = new Paragraph(chunk);
        paragraph.setAlignment(Element.ALIGN_RIGHT);
        return paragraph;
    }

    Paragraph spacer() {
        return new Paragraph(" ", textFont);
    }

    Chunk text(String value) {
        return new Chunk(value, textFont);
    }

    Chunk bold(String value) {
        return new Chunk(value != null ? value : "-", boldFont);
    }

    private static byte[] loadLogoHouse() {
        try (InputStream in = new ClassPathResource(LOGO_PATH).getInputStream()) {
            BufferedImage full = ImageIO.read(in);
            int x = (int) (full.getWidth() * LOGO_HOUSE_REGION[0]);
            int y = (int) (full.getHeight() * LOGO_HOUSE_REGION[1]);
            int width = (int) (full.getWidth() * LOGO_HOUSE_REGION[2]);
            int height = (int) (full.getHeight() * LOGO_HOUSE_REGION[3]);

            int targetHeight = Math.round(LOGO_WIDTH_PX * (float) height / width);
            BufferedImage house = new BufferedImage(LOGO_WIDTH_PX, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = house.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(full, 0, 0, LOGO_WIDTH_PX, targetHeight, x, y, x + width, y + height, null);
            g.dispose();

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(house, "png", png);
            return png.toByteArray();
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo {} para las cartas; se generan sin logo.", LOGO_PATH, e);
            return null;
        }
    }
}
