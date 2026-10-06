package com.davivienda.factoraje.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.upload_resource.ResourceFile;
import com.davivienda.factoraje.dto.upload_resource.UploadResourcesDTOResponse;
import com.davivienda.factoraje.service.UploadResourceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/upload-resources")
@RequiredArgsConstructor
public class UploadResourceController {

    static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final UploadResourceService uploadResourceService;

    @GetMapping
    public ResponseEntity<ApiResponse<UploadResourcesDTOResponse>> getResources() {
        return ResponseEntity.ok(ApiResponse.success(
                uploadResourceService.getResources(), "Recursos de carga obtenidos exitosamente."));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        return attachment(uploadResourceService.getTemplate(), XLSX);
    }

    @GetMapping("/manual")
    public ResponseEntity<byte[]> downloadManual() {
        return attachment(uploadResourceService.getManual(), MediaType.APPLICATION_PDF);
    }

    @PutMapping(value = "/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResourcesDTOResponse.ResourceInfo>> replaceTemplate(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                uploadResourceService.replaceTemplate(file), "Plantilla publicada correctamente."));
    }

    @PutMapping(value = "/manual", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResourcesDTOResponse.ResourceInfo>> replaceManual(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(
                uploadResourceService.replaceManual(file), "Manual publicado correctamente."));
    }

    /** Siempre como adjunto, para que el navegador no interprete el archivo dentro del sitio. */
    private static ResponseEntity<byte[]> attachment(ResourceFile file, MediaType mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
