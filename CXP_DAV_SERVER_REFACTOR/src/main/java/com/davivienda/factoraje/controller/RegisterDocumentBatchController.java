package com.davivienda.factoraje.controller;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.domain.enums.TermTypeUniqueCodeEnum;
import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTORequest;
import com.davivienda.factoraje.dto.upload_batch.BatchProcessResult;
import com.davivienda.factoraje.dto.upload_batch.UploadSettingsDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.InvalidFileException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.service.RegisterDocumentBatchService;
import com.davivienda.factoraje.service.TermVersionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/batches")
@Validated
@RequiredArgsConstructor
public class RegisterDocumentBatchController {

    private final RegisterDocumentBatchService registerDocumentBatchService;
    private final TermVersionService termVersionService;
    private final CurrentUserService currentUser;
    private final SystemParameters systemParameters;
    private final MailNoticePublisher mailNotices;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> uploadBatch(
            @RequestParam("file") @NotNull(message = "El archivo es obligatorio.") MultipartFile file,
            @RequestParam("payerId") @NotNull(message = "El ID del pagador es obligatorio.") UUID payerId,
            @RequestParam(value = "termVersionId", required = false) UUID termVersionId,
            HttpServletRequest request) {

        currentUser.requireOwnEntity(payerId);
        UUID userId = currentUser.id();
        boolean uploadedByAdmin = currentUser.isAdmin();
        String uploaderName = currentUser.get().fullName();

        log.info("Iniciando carga de documentos del pagador {} por el usuario {}", payerId, userId);

        try {
            validateFile(file);
        } catch (InvalidFileException ex) {
            mailNotices.uploadFailed(payerId, null, file.getOriginalFilename(), uploaderName, ex.getMessage());
            throw ex;
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            userAgent = "Unknown-Agent";
        }

        TermVersionCat acceptedPayerTerm = termVersionId != null
                ? termVersionService.requireActiveVersion(termVersionId, TermTypeUniqueCodeEnum.PAYER_TERM_TYPE)
                : termVersionService.getActiveEntity(TermTypeUniqueCodeEnum.PAYER_TERM_TYPE);

        AcceptanceAuditDTORequest auditRequest = new AcceptanceAuditDTORequest(userAgent, userId,
                acceptedPayerTerm.getId());

        BatchProcessResult result = registerDocumentBatchService.processFile(
                file, auditRequest, payerId, userId, uploadedByAdmin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        // El cliente decide con esta cabecera si el PDF es un comprobante o un reporte de rechazo.
        headers.add("X-Batch-Status", result.isSuccess() ? "SUCCESS" : "REJECTED");
        headers.setContentDispositionFormData("attachment",
                result.isSuccess() ? "Comprobante_Carga.pdf" : "Reporte_Inconsistencias.pdf");

        return result.isSuccess()
                ? ResponseEntity.ok().headers(headers).body(result.pdfContent())
                : ResponseEntity.badRequest().headers(headers).body(result.pdfContent());
    }

    @GetMapping("/upload-settings")
    public ResponseEntity<ApiResponse<UploadSettingsDTOResponse>> uploadSettings() {
        UploadSettingsDTOResponse settings = new UploadSettingsDTOResponse(
                systemParameters.getList(SystemParameterKey.UPLOAD_ALLOWED_EXTENSIONS),
                systemParameters.getInt(SystemParameterKey.UPLOAD_MAX_FILE_SIZE_MB),
                systemParameters.getInt(SystemParameterKey.UPLOAD_MAX_ROWS));
        return ResponseEntity.ok(ApiResponse.success(settings, "Configuración de carga."));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Es obligatorio adjuntar un archivo Excel no vacío.");
        }

        List<String> allowedExtensions = systemParameters.getList(SystemParameterKey.UPLOAD_ALLOWED_EXTENSIONS);
        String originalFilename = file.getOriginalFilename();
        String lowerName = originalFilename != null ? originalFilename.toLowerCase(Locale.ROOT) : "";
        if (allowedExtensions.stream().noneMatch(lowerName::endsWith)) {
            throw new InvalidFileException("Formato de archivo no permitido. Extensiones aceptadas: "
                    + String.join(", ", allowedExtensions) + ".");
        }

        int maxFileSizeMb = systemParameters.getInt(SystemParameterKey.UPLOAD_MAX_FILE_SIZE_MB);
        if (file.getSize() > maxFileSizeMb * 1024L * 1024L) {
            throw new InvalidFileException(
                    "El archivo supera el tamaño máximo permitido de " + maxFileSizeMb + " MB.");
        }
    }
}
