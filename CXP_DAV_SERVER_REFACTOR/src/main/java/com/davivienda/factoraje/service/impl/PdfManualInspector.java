package com.davivienda.factoraje.service.impl;

import java.io.IOException;
import java.util.Set;

import com.davivienda.factoraje.infrastructure.exception.InvalidFileException;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;

/**
 * Rechaza los PDF con contenido activo. Se recorren todos los objetos del documento, incluidos los
 * de flujos de objetos comprimidos, porque buscar texto en los bytes no detecta esos casos.
 */
final class PdfManualInspector {

    static final String UNREADABLE_MESSAGE =
            "El manual no pudo ser leído. Verifique que sea un PDF válido y sin contraseña.";

    private static final PdfName JS = new PdfName("JS");
    private static final PdfName JAVASCRIPT = new PdfName("JavaScript");
    private static final PdfName XFA = new PdfName("XFA");
    private static final PdfName EMBEDDED_FILE = new PdfName("EmbeddedFile");
    private static final PdfName EMBEDDED_FILES = new PdfName("EmbeddedFiles");
    private static final PdfName EF = new PdfName("EF");
    private static final PdfName RICH_MEDIA = new PdfName("RichMedia");
    private static final Set<PdfName> FORBIDDEN_ACTIONS = Set.of(JAVASCRIPT, new PdfName("Launch"),
            new PdfName("ImportData"), new PdfName("SubmitForm"), new PdfName("GoToE"),
            new PdfName("RichMediaExecute"));

    /** Los objetos directos se anidan poco; el límite evita recursión ilimitada con archivos maliciosos. */
    private static final int MAX_DEPTH = 32;

    private PdfManualInspector() {
    }

    static void inspect(byte[] content) {
        try (PdfReader reader = new PdfReader(content)) {
            if (reader.isEncrypted()) {
                throw new InvalidFileException("El manual no puede estar protegido ni cifrado.");
            }
            for (int i = 1; i < reader.getXrefSize(); i++) {
                visit(reader.getPdfObjectRelease(i), 0);
            }
        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException | RuntimeException | NoClassDefFoundError e) {
            // Algunos tipos de cifrado requieren BouncyCastle, que el proyecto no incluye; esos PDF
            // también se rechazan.
            throw new InvalidFileException(UNREADABLE_MESSAGE, e);
        }
    }

    /** Las referencias indirectas no se siguen porque cada objeto indirecto se visita en el ciclo principal. */
    private static void visit(PdfObject object, int depth) {
        if (object == null || depth > MAX_DEPTH) {
            return;
        }
        if (object instanceof PdfDictionary dictionary) {
            check(dictionary);
            for (PdfName key : dictionary.getKeys()) {
                visit(dictionary.get(key), depth + 1);
            }
        } else if (object instanceof PdfArray array) {
            for (int i = 0; i < array.size(); i++) {
                visit(array.getPdfObject(i), depth + 1);
            }
        }
    }

    private static void check(PdfDictionary dictionary) {
        PdfObject action = dictionary.get(PdfName.S);
        if (dictionary.contains(JS) || dictionary.contains(JAVASCRIPT) || dictionary.contains(XFA)
                || (action != null && FORBIDDEN_ACTIONS.contains(action))) {
            throw new InvalidFileException(
                    "El manual no puede contener JavaScript ni acciones que ejecuten programas o envíen datos.");
        }
        if (EMBEDDED_FILE.equals(dictionary.get(PdfName.TYPE)) || dictionary.contains(EMBEDDED_FILES)
                || dictionary.contains(EF) || dictionary.contains(RICH_MEDIA)) {
            throw new InvalidFileException("El manual no puede contener archivos incrustados.");
        }
    }
}
