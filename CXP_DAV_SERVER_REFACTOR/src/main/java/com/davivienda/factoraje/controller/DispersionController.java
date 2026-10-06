package com.davivienda.factoraje.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchDetailDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchHistoryDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchSummaryDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionDocumentDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionPayerDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionRequestDTOResponse;
import com.davivienda.factoraje.dto.dispersion.GenerateDispersionBatchRequestDTO;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.service.DispersionBatchService;
import com.davivienda.factoraje.service.DispersionQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Dispersión de documentos no financiables con cargo a la cuenta del pagador. */
@Slf4j
@RestController
@RequestMapping("/api/v1/dispersion-requests")
@RequiredArgsConstructor
public class DispersionController {

    private final DispersionQueryService dispersionQueryService;
    private final DispersionBatchService dispersionBatchService;
    private final CurrentUserService currentUser;

    // ---------- Consultas ----------

    @GetMapping("/payers")
    public ResponseEntity<ApiResponse<List<DispersionPayerDTOResponse>>> getPayers() {
        return ResponseEntity.ok(ApiResponse.success(dispersionQueryService.getPayers(),
                "Pagadores obtenidos exitosamente"));
    }

    @GetMapping("/payers/{payerId}/requests")
    public ResponseEntity<ApiResponse<List<DispersionRequestDTOResponse>>> getPayerRequests(
            @PathVariable UUID payerId) {
        return ResponseEntity.ok(ApiResponse.success(dispersionQueryService.getPayerRequests(payerId),
                "Solicitudes de dispersión obtenidas exitosamente"));
    }

    @GetMapping("/payers/{payerId}/requests/documents")
    public ResponseEntity<ApiResponse<List<DispersionDocumentDTOResponse>>> getPendingDocuments(
            @PathVariable UUID payerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        return ResponseEntity.ok(ApiResponse.success(dispersionQueryService.getPendingDocuments(payerId, dueDate),
                "Documentos por dispersar obtenidos exitosamente"));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<DispersionBatchHistoryDTOResponse>> getBatchHistory(
            @RequestParam(required = false) UUID payerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(dispersionQueryService.getBatchHistory(payerId, page, size),
                "Bitácora de dispersiones obtenida exitosamente"));
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<DispersionBatchDetailDTOResponse>> getBatchDetails(@PathVariable UUID batchId) {
        return ResponseEntity.ok(ApiResponse.success(dispersionQueryService.getBatchDetails(batchId),
                "Detalle del lote de dispersión obtenido exitosamente"));
    }

    @GetMapping(value = "/batches/{batchId}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadLetter(@PathVariable UUID batchId) {
        return pdfResponse(dispersionBatchService.regenerateLetter(batchId));
    }

    // ---------- Comandos ----------

    @PostMapping(value = "/generate-batch", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateBatch(@Valid @RequestBody GenerateDispersionBatchRequestDTO request) {

        log.info("Generando lote de dispersión del pagador {} con vencimiento {}", request.payerId(), request.dueDate());

        return pdfResponse(dispersionBatchService.generateBatch(
                currentUser.id(), request.payerId(), request.dueDate()));
    }

    @PostMapping("/batches/{batchId}/confirm")
    public ResponseEntity<ApiResponse<DispersionBatchSummaryDTOResponse>> confirmBatch(@PathVariable UUID batchId) {

        DispersionBatchSummaryDTOResponse batch = dispersionBatchService.confirmBatch(batchId, currentUser.id());

        return ResponseEntity.ok(ApiResponse.success(batch, "La dispersión del lote ha sido confirmada."));
    }

    private ResponseEntity<byte[]> pdfResponse(DispersionBatchService.Letter letter) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(letter.fileName()).build().toString())
                .body(letter.content());
    }
}
