package com.davivienda.factoraje.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.davivienda.factoraje.domain.entities.ExcelTemplateColumnModel;
import com.davivienda.factoraje.domain.entities.UploadResourceModel;
import com.davivienda.factoraje.domain.enums.UploadResourceType;
import com.davivienda.factoraje.dto.upload_resource.ResourceFile;
import com.davivienda.factoraje.dto.upload_resource.UploadResourcesDTOResponse;
import com.davivienda.factoraje.dto.upload_resource.UploadResourcesDTOResponse.ResourceInfo;
import com.davivienda.factoraje.infrastructure.exception.InvalidFileException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.ExcelTemplateColumnRepository;
import com.davivienda.factoraje.repository.UploadResourceRepository;
import com.davivienda.factoraje.service.UploadResourceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadResourceServiceImpl implements UploadResourceService {

    static final FileRules TEMPLATE_RULES = new FileRules("la plantilla", ".xlsx", FileMagic.OOXML,
            1024L * 1024L, "1 MB", "Plantilla_Carga_Documentos.xlsx");
    static final FileRules MANUAL_RULES = new FileRules("el manual", ".pdf", FileMagic.PDF,
            10L * 1024L * 1024L, "10 MB", "Manual_Carga_Documentos.pdf");
    private static final String UNREADABLE_TEMPLATE_MESSAGE =
            "La plantilla no pudo ser leída. Verifique que sea un archivo .xlsx válido.";

    private final UploadResourceRepository uploadResourceRepository;
    private final ExcelTemplateColumnRepository excelTemplateColumnRepository;
    private final CurrentUserService currentUser;
    private final MailNoticePublisher mailNotices;

    /** Reglas comunes de un recurso; {@code label} va en minúscula con artículo para armar los mensajes. */
    record FileRules(String label, String extension, FileMagic magic, long maxBytes, String maxSizeLabel,
            String defaultName) {
    }

    @Override
    @Transactional(readOnly = true)
    public UploadResourcesDTOResponse getResources() {
        ResourceInfo template = null;
        ResourceInfo manual = null;
        for (UploadResourceRepository.Summary summary : uploadResourceRepository.findSummaries()) {
            ResourceInfo info = new ResourceInfo(summary.getFileName(), summary.getFileSize(), summary.getUpdatedAt(),
                    fullName(summary.getUpdatedByFirstName(), summary.getUpdatedByLastName()));
            if (summary.getType() == UploadResourceType.TEMPLATE) {
                template = info;
            } else {
                manual = info;
            }
        }
        return new UploadResourcesDTOResponse(template, manual);
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceFile getTemplate() {
        return download(UploadResourceType.TEMPLATE, "No hay una plantilla de carga publicada.");
    }

    @Override
    @Transactional(readOnly = true)
    public ResourceFile getManual() {
        return download(UploadResourceType.MANUAL, "No hay un manual de carga publicado.");
    }

    @Override
    @Transactional
    public ResourceInfo replaceTemplate(MultipartFile file) {
        byte[] content = readFile(file, TEMPLATE_RULES);
        validateHeaders(readHeaders(content), activeColumnNames());
        return replace(UploadResourceType.TEMPLATE, file, content, TEMPLATE_RULES);
    }

    @Override
    @Transactional
    public ResourceInfo replaceManual(MultipartFile file) {
        byte[] content = readFile(file, MANUAL_RULES);
        PdfManualInspector.inspect(content);
        return replace(UploadResourceType.MANUAL, file, content, MANUAL_RULES);
    }

    private ResourceFile download(UploadResourceType type, String notFoundMessage) {
        UploadResourceModel resource = uploadResourceRepository.findByType(type)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage));
        return new ResourceFile(resource.getFileName(), resource.getFileContent());
    }

    private ResourceInfo replace(UploadResourceType type, MultipartFile file, byte[] content, FileRules rules) {
        UploadResourceModel resource = uploadResourceRepository.findByType(type)
                .orElseGet(() -> UploadResourceModel.builder().type(type).build());
        String resourceLabel = type == UploadResourceType.TEMPLATE ? "Plantilla de carga" : "Manual de carga";
        String previous = resource.getFileName() != null
                ? resourceLabel + ": " + resource.getFileName()
                : MailNoticePublisher.NO_RECORD;
        resource.setFileName(safeFileName(file.getOriginalFilename(), rules));
        resource.setFileContent(content);
        resource.setFileSize((long) content.length);
        resource.setUpdatedBy(currentUser.managed());
        UploadResourceModel saved = uploadResourceRepository.saveAndFlush(resource);

        log.info("Recurso de carga {} reemplazado por el usuario {} ({} bytes)", type, currentUser.id(), content.length);
        mailNotices.operatorChanged("Recursos de carga", previous, resourceLabel + ": " + resource.getFileName());
        return new ResourceInfo(saved.getFileName(), saved.getFileSize(), saved.getUpdatedAt(),
                fullName(saved.getUpdatedBy().getFirstName(), saved.getUpdatedBy().getLastName()));
    }

    static byte[] readFile(MultipartFile file, FileRules rules) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Es obligatorio adjuntar " + rules.label() + ".");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(rules.extension())) {
            throw new InvalidFileException(capitalize(rules.label()) + " debe ser un archivo " + rules.extension() + ".");
        }
        if (file.getSize() > rules.maxBytes()) {
            throw new InvalidFileException(capitalize(rules.label()) + " supera el tamaño máximo permitido de "
                    + rules.maxSizeLabel() + ".");
        }
        String invalidMessage = capitalize(rules.label()) + " no es un archivo " + rules.extension() + " válido.";
        try {
            byte[] content = file.getBytes();
            try (InputStream stream = FileMagic.prepareToCheckMagic(new ByteArrayInputStream(content))) {
                if (FileMagic.valueOf(stream) != rules.magic()) {
                    throw new InvalidFileException(invalidMessage);
                }
            }
            return content;
        } catch (IOException e) {
            throw new InvalidFileException(invalidMessage, e);
        }
    }

    /** Encabezados no vacíos de la primera fila de la primera hoja, que es lo único que lee la carga. */
    static List<String> readHeaders(byte[] content) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            if (hasMacros(workbook)) {
                throw new InvalidFileException("La plantilla no puede contener macros.");
            }
            Row headerRow = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0).getRow(0) : null;
            if (headerRow == null) {
                throw new InvalidFileException("La primera fila de la primera hoja debe contener los encabezados.");
            }
            DataFormatter formatter = new DataFormatter();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).trim();
                if (!header.isEmpty()) {
                    headers.add(header);
                }
            }
            return headers;
        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException | InvalidFormatException | RuntimeException e) {
            throw new InvalidFileException(UNREADABLE_TEMPLATE_MESSAGE, e);
        }
    }

    private static boolean hasMacros(XSSFWorkbook workbook) throws InvalidFormatException {
        if (workbook.isMacroEnabled()) {
            return true;
        }
        for (PackagePart part : workbook.getPackage().getParts()) {
            if (part.getPartName().getName().toLowerCase(Locale.ROOT).endsWith("vbaproject.bin")) {
                return true;
            }
        }
        return false;
    }

    /** Igual que la carga, los encabezados se comparan sin distinguir mayúsculas ni espacios en los extremos. */
    static void validateHeaders(List<String> headers, List<String> expected) {
        Map<String, String> expectedByKey = new LinkedHashMap<>();
        expected.forEach(name -> expectedByKey.put(key(name), name));

        Set<String> seen = new LinkedHashSet<>();
        List<String> duplicated = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (String header : headers) {
            if (!seen.add(key(header))) {
                duplicated.add(header);
            } else if (!expectedByKey.containsKey(key(header))) {
                unknown.add(header);
            }
        }
        List<String> missing = expectedByKey.entrySet().stream()
                .filter(entry -> !seen.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (missing.isEmpty() && unknown.isEmpty() && duplicated.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder("La plantilla no coincide con las columnas que acepta la carga.");
        if (!missing.isEmpty()) {
            message.append(" Faltan: ").append(String.join(", ", missing)).append('.');
        }
        if (!unknown.isEmpty()) {
            message.append(" No reconocidas: ").append(String.join(", ", unknown)).append('.');
        }
        if (!duplicated.isEmpty()) {
            message.append(" Repetidas: ").append(String.join(", ", duplicated)).append('.');
        }
        throw new InvalidFileException(message.toString());
    }

    private List<String> activeColumnNames() {
        return excelTemplateColumnRepository.findByActiveTrue().stream()
                .map(ExcelTemplateColumnModel::getExcelColumnName)
                .toList();
    }

    private static String key(String header) {
        return header.trim().toLowerCase(Locale.ROOT);
    }

    /** Solo se conserva el nombre, sin rutas ni caracteres de control, para mostrarlo y descargarlo. */
    static String safeFileName(String originalFilename, FileRules rules) {
        if (originalFilename == null) {
            return rules.defaultName();
        }
        String name = originalFilename.substring(Math.max(originalFilename.lastIndexOf('/'),
                originalFilename.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (name.length() > 255) {
            name = name.substring(name.length() - 255);
        }
        return name.toLowerCase(Locale.ROOT).endsWith(rules.extension()) && name.length() > rules.extension().length()
                ? name
                : rules.defaultName();
    }

    private static String capitalize(String text) {
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String fullName(String firstName, String lastName) {
        String name = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        return name.isEmpty() ? null : name;
    }
}
