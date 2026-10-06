package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.davivienda.factoraje.domain.entities.ExcelTemplateColumnModel;
import com.davivienda.factoraje.domain.entities.UploadResourceModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.UploadResourceType;
import com.davivienda.factoraje.dto.upload_resource.UploadResourcesDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.InvalidFileException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.ExcelTemplateColumnRepository;
import com.davivienda.factoraje.repository.UploadResourceRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfAction;
import com.lowagie.text.pdf.PdfFileSpecification;
import com.lowagie.text.pdf.PdfWriter;

@ExtendWith(MockitoExtension.class)
class UploadResourceServiceImplTest {

    private static final List<String> ACTIVE_COLUMNS = List.of("Fecha Emision", "Monto", "NIT Proveedor");

    @Mock
    private UploadResourceRepository uploadResourceRepository;
    @Mock
    private ExcelTemplateColumnRepository excelTemplateColumnRepository;
    @Mock
    private CurrentUserService currentUser;
    @Mock
    private MailNoticePublisher mailNotices;

    @InjectMocks
    private UploadResourceServiceImpl service;

    private final UserModel admin = UserModel.builder().id(UUID.randomUUID()).firstName("Ana").lastName("Operadora").build();

    @FunctionalInterface
    private interface WriterSetup {
        void apply(PdfWriter writer) throws Exception;
    }

    @BeforeEach
    void setUp() {
        lenient().when(excelTemplateColumnRepository.findByActiveTrue()).thenReturn(ACTIVE_COLUMNS.stream()
                .map(name -> ExcelTemplateColumnModel.builder().excelColumnName(name).active(true).build())
                .toList());
        lenient().when(currentUser.managed()).thenReturn(admin);
        lenient().when(uploadResourceRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void publishesATemplateWhoseHeadersMatchTheActiveColumnsInAnyOrderAndCase() throws IOException {
        when(uploadResourceRepository.findByType(UploadResourceType.TEMPLATE)).thenReturn(Optional.empty());
        byte[] content = workbook(XSSFWorkbookType.XLSX, " nit proveedor ", "MONTO", "Fecha Emision");

        UploadResourcesDTOResponse.ResourceInfo info = service.replaceTemplate(
                new MockMultipartFile("file", "C:\\Users\\ana\\Plantilla Carga.xlsx", null, content));

        ArgumentCaptor<UploadResourceModel> saved = ArgumentCaptor.forClass(UploadResourceModel.class);
        verify(uploadResourceRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(UploadResourceType.TEMPLATE);
        assertThat(saved.getValue().getFileContent()).isEqualTo(content);
        assertThat(saved.getValue().getUpdatedBy()).isSameAs(admin);
        assertThat(info.fileName()).isEqualTo("Plantilla Carga.xlsx");
        assertThat(info.fileSize()).isEqualTo(content.length);
        assertThat(info.updatedBy()).isEqualTo("Ana Operadora");
    }

    @Test
    void replacesTheCurrentTemplateInsteadOfCreatingAnotherOne() throws IOException {
        UploadResourceModel current = UploadResourceModel.builder()
                .id(UUID.randomUUID()).type(UploadResourceType.TEMPLATE).fileName("anterior.xlsx").build();
        when(uploadResourceRepository.findByType(UploadResourceType.TEMPLATE)).thenReturn(Optional.of(current));

        service.replaceTemplate(file("nueva.xlsx", workbook(XSSFWorkbookType.XLSX, ACTIVE_COLUMNS.toArray(String[]::new))));

        assertThat(current.getFileName()).isEqualTo("nueva.xlsx");
        verify(uploadResourceRepository).saveAndFlush(current);
    }

    @Test
    void rejectsATemplateThatDoesNotMatchTheColumnsOfTheUpload() throws IOException {
        byte[] content = workbook(XSSFWorkbookType.XLSX, "Fecha Emision", "Monto", "Monto ", "NIU Proveedor");

        assertThatThrownBy(() -> service.replaceTemplate(file("plantilla.xlsx", content)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("La plantilla no coincide con las columnas que acepta la carga. Faltan: NIT Proveedor. "
                        + "No reconocidas: NIU Proveedor. Repetidas: Monto.");
        verify(uploadResourceRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsMacroEnabledWorkbooks() throws IOException {
        byte[] content = workbook(XSSFWorkbookType.XLSM, ACTIVE_COLUMNS.toArray(String[]::new));

        assertThatThrownBy(() -> service.replaceTemplate(file("plantilla.xlsx", content)))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("La plantilla no puede contener macros.");
    }

    @Test
    void rejectsFilesThatAreNotXlsx() {
        assertThatThrownBy(() -> service.replaceTemplate(file("plantilla.xls", new byte[] { 1, 2, 3 })))
                .hasMessage("La plantilla debe ser un archivo .xlsx.");
        assertThatThrownBy(() -> service.replaceTemplate(file("plantilla.xlsx", "<html></html>".getBytes())))
                .hasMessage("La plantilla no es un archivo .xlsx válido.");
        assertThatThrownBy(() -> service.replaceTemplate(file("plantilla.xlsx", new byte[0])))
                .hasMessage("Es obligatorio adjuntar la plantilla.");
    }

    @Test
    void rejectsTemplatesLargerThanOneMegabyte() {
        byte[] content = new byte[(int) UploadResourceServiceImpl.TEMPLATE_RULES.maxBytes() + 1];

        assertThatThrownBy(() -> service.replaceTemplate(file("plantilla.xlsx", content)))
                .hasMessage("La plantilla supera el tamaño máximo permitido de 1 MB.");
    }

    @Test
    void fileNamesKeepOnlyTheNameWithoutPathsOrControlCharacters() {
        var rules = UploadResourceServiceImpl.TEMPLATE_RULES;
        assertThat(UploadResourceServiceImpl.safeFileName("../../etc/plantilla.xlsx", rules)).isEqualTo("plantilla.xlsx");
        assertThat(UploadResourceServiceImpl.safeFileName("plan\"ti\nlla.xlsx", rules)).isEqualTo("plantilla.xlsx");
        assertThat(UploadResourceServiceImpl.safeFileName(".xlsx", rules)).isEqualTo(rules.defaultName());
        assertThat(UploadResourceServiceImpl.safeFileName(null, rules)).isEqualTo(rules.defaultName());
        assertThat(UploadResourceServiceImpl.safeFileName("manual.xlsx", UploadResourceServiceImpl.MANUAL_RULES))
                .isEqualTo(UploadResourceServiceImpl.MANUAL_RULES.defaultName());
    }

    @Test
    void downloadingWithoutPublishedResourcesFails() {
        when(uploadResourceRepository.findByType(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTemplate())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No hay una plantilla de carga publicada.");
        assertThatThrownBy(() -> service.getManual())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No hay un manual de carga publicado.");
    }

    @Test
    void publishesAPlainPdfManual() throws Exception {
        when(uploadResourceRepository.findByType(UploadResourceType.MANUAL)).thenReturn(Optional.empty());
        byte[] content = pdf(writer -> { });

        UploadResourcesDTOResponse.ResourceInfo info = service.replaceManual(file("Manual de carga.pdf", content));

        ArgumentCaptor<UploadResourceModel> saved = ArgumentCaptor.forClass(UploadResourceModel.class);
        verify(uploadResourceRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getType()).isEqualTo(UploadResourceType.MANUAL);
        assertThat(saved.getValue().getFileContent()).isEqualTo(content);
        assertThat(info.fileName()).isEqualTo("Manual de carga.pdf");
        assertThat(info.updatedBy()).isEqualTo("Ana Operadora");
    }

    @Test
    void rejectsManualsWithJavaScriptOrLaunchActions() throws Exception {
        byte[] withScript = pdf(writer -> writer.addJavaScript("app.alert('hola');"));
        byte[] withLaunch = pdf(writer -> writer.setOpenAction(new PdfAction("calc.exe", null, null, null)));

        assertThatThrownBy(() -> service.replaceManual(file("manual.pdf", withScript)))
                .hasMessageStartingWith("El manual no puede contener JavaScript");
        assertThatThrownBy(() -> service.replaceManual(file("manual.pdf", withLaunch)))
                .hasMessageStartingWith("El manual no puede contener JavaScript");
        verify(uploadResourceRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsManualsWithEmbeddedFiles() throws Exception {
        byte[] content = pdf(writer -> writer.addFileAttachment(PdfFileSpecification.fileEmbedded(
                writer, null, "datos.exe", "MZ".getBytes(StandardCharsets.US_ASCII))));

        assertThatThrownBy(() -> service.replaceManual(file("manual.pdf", content)))
                .hasMessage("El manual no puede contener archivos incrustados.");
    }

    /**
     * OpenPDF necesita BouncyCastle para generar PDF cifrados, así que este se escribe a mano. Según
     * cómo falle el descifrado, el rechazo es por cifrado o por no poder leerlo; ambos lo descartan.
     */
    @Test
    void rejectsEncryptedManuals() {
        String zeros = "0".repeat(64);
        byte[] content = ("%PDF-1.4\n"
                + "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
                + "2 0 obj\n<< /Type /Pages /Kids [] /Count 0 >>\nendobj\n"
                + "3 0 obj\n<< /Filter /Standard /V 2 /R 3 /Length 128 /P -4 /O <" + zeros + "> /U <" + zeros + "> >>\nendobj\n"
                + "trailer\n<< /Size 4 /Root 1 0 R /Encrypt 3 0 R /ID [<" + zeros + "> <" + zeros + ">] >>\n"
                + "%%EOF\n").getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> service.replaceManual(file("manual.pdf", content)))
                .isInstanceOf(InvalidFileException.class)
                .satisfies(error -> assertThat(error.getMessage()).isIn(
                        "El manual no puede estar protegido ni cifrado.", PdfManualInspector.UNREADABLE_MESSAGE));
        verify(uploadResourceRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsManualsThatAreNotPdf() {
        assertThatThrownBy(() -> service.replaceManual(file("manual.docx", new byte[] { 1, 2, 3 })))
                .hasMessage("El manual debe ser un archivo .pdf.");
        assertThatThrownBy(() -> service.replaceManual(file("manual.pdf", "<html></html>".getBytes())))
                .hasMessage("El manual no es un archivo .pdf válido.");
        assertThatThrownBy(() -> service.replaceManual(file("manual.pdf", "%PDF-1.4 truncado".getBytes())))
                .hasMessage(PdfManualInspector.UNREADABLE_MESSAGE);
        assertThatThrownBy(() -> service.replaceManual(
                file("manual.pdf", new byte[(int) UploadResourceServiceImpl.MANUAL_RULES.maxBytes() + 1])))
                .hasMessage("El manual supera el tamaño máximo permitido de 10 MB.");
    }

    @Test
    void resourcesNotPublishedYetAreNull() {
        when(uploadResourceRepository.findSummaries()).thenReturn(List.of(summary(UploadResourceType.MANUAL, "Manual.pdf")));

        UploadResourcesDTOResponse resources = service.getResources();

        assertThat(resources.template()).isNull();
        assertThat(resources.manual().fileName()).isEqualTo("Manual.pdf");
        assertThat(resources.manual().updatedBy()).isNull();
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, null, content);
    }

    private static byte[] workbook(XSSFWorkbookType type, String... headers) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(type); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFRow row = workbook.createSheet("Documentos").createRow(0);
            for (int i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(headers[i]);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /** Los adjuntos solo pueden agregarse con el documento abierto. */
    private static byte[] pdf(WriterSetup afterOpen) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        document.open();
        afterOpen.apply(writer);
        document.add(new Paragraph("Manual de carga"));
        document.close();
        return out.toByteArray();
    }

    private static UploadResourceRepository.Summary summary(UploadResourceType type, String fileName) {
        return new UploadResourceRepository.Summary() {
            public UploadResourceType getType() { return type; }
            public String getFileName() { return fileName; }
            public Long getFileSize() { return 8L; }
            public Instant getUpdatedAt() { return Instant.now(); }
            public String getUpdatedByFirstName() { return null; }
            public String getUpdatedByLastName() { return null; }
        };
    }
}
