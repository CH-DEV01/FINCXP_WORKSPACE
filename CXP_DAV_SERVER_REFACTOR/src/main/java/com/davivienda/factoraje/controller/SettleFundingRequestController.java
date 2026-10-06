package com.davivienda.factoraje.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
import com.davivienda.factoraje.dto.disbursement_batch.ConfirmDisbursementRequestDTO;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchDetailDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchHistoryDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchSummaryDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementGroupDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestSupplierDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.GenerateBatchRequestDTO;
import com.davivienda.factoraje.dto.disbursement_batch.PayerDisbursementResumeDTOResponse;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.service.DisbursementBatchService;
import com.davivienda.factoraje.service.DisbursementConfirmationService;
import com.davivienda.factoraje.service.DisbursementQueryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/disbursement-requests")
@RequiredArgsConstructor
public class SettleFundingRequestController {

    private final DisbursementQueryService disbursementQueryService;
    private final DisbursementBatchService disbursementBatchService;
    private final DisbursementConfirmationService disbursementConfirmationService;
    private final CurrentUserService currentUser;

    // ---------- Consultas ----------

    @GetMapping("/payers")
    public ResponseEntity<ApiResponse<List<PayerDisbursementResumeDTOResponse>>> getPayersResume() {
        List<PayerDisbursementResumeDTOResponse> payers = disbursementQueryService.getPayersResume();
        return ResponseEntity.ok(ApiResponse.success(payers, "Resumen de pagadores obtenido exitosamente"));
    }

    @GetMapping("/payers/{payerId}/groups")
    public ResponseEntity<ApiResponse<List<DisbursementGroupDTOResponse>>> getDisbursementGroups(
            @PathVariable UUID payerId) {
        List<DisbursementGroupDTOResponse> groups = disbursementQueryService.getDisbursementGroups(payerId);
        return ResponseEntity.ok(ApiResponse.success(groups, "Combinaciones de desembolso obtenidas exitosamente"));
    }

    @GetMapping("/payers/{payerId}/requests")
    public ResponseEntity<ApiResponse<List<DisbursementRequestDTOResponse>>> getPayerRequests(
            @PathVariable UUID payerId) {
        List<DisbursementRequestDTOResponse> requests = disbursementQueryService.getPayerRequests(payerId);
        return ResponseEntity.ok(ApiResponse.success(requests, "Solicitudes de desembolso obtenidas exitosamente"));
    }

    @GetMapping("/payers/{payerId}/requests/suppliers")
    public ResponseEntity<ApiResponse<List<DisbursementRequestSupplierDTOResponse>>> getRequestSuppliers(
            @PathVariable UUID payerId,
            @RequestParam(required = false) UUID batchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate disbursementDate) {
        List<DisbursementRequestSupplierDTOResponse> suppliers = disbursementQueryService
                .getRequestSuppliers(payerId, batchId, dueDate, requestDate, disbursementDate);
        return ResponseEntity.ok(ApiResponse.success(suppliers, "Detalle de la solicitud obtenido exitosamente"));
    }

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<DisbursementBatchHistoryDTOResponse>> getBatchHistory(
            @RequestParam(required = false) UUID payerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        DisbursementBatchHistoryDTOResponse history = disbursementQueryService.getBatchHistory(payerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(history, "Bitácora de lotes obtenida exitosamente"));
    }

    @GetMapping("/batches/{batchId}")
    public ResponseEntity<ApiResponse<DisbursementBatchDetailDTOResponse>> getBatchDetails(
            @PathVariable UUID batchId) {
        DisbursementBatchDetailDTOResponse detail = disbursementQueryService.getBatchDetails(batchId);
        return ResponseEntity.ok(ApiResponse.success(detail, "Detalle del lote obtenido exitosamente"));
    }

    @GetMapping(value = "/batches/{batchId}/download", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadBatchReports(@PathVariable UUID batchId) {

        log.info("Re-generando reporte del lote de desembolso: {}", batchId);

        return pdfResponse(disbursementBatchService.regenerateBatchReport(batchId));
    }

    // ---------- Comandos ----------

    @PostMapping(value = "/generate-batch", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateDisbursementBatch(
            @Valid @RequestBody GenerateBatchRequestDTO request) {

        log.info("Generando lote de desembolso para el pagador {}", request.payerId());

        Map<String, byte[]> reports = disbursementBatchService.generateDisbursementBatches(
                currentUser.id(),
                request.originalFileName(),
                request.payerId(),
                request.groups());

        return pdfResponse(reports);
    }

    private ResponseEntity<byte[]> pdfResponse(Map<String, byte[]> reports) {
        Map.Entry<String, byte[]> report = reports.entrySet().iterator().next();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(report.getKey()).build().toString())
                .body(report.getValue());
    }

    @PostMapping("/batches/{batchId}/confirm")
    public ResponseEntity<ApiResponse<DisbursementBatchSummaryDTOResponse>> confirmDisbursementBatch(
            @PathVariable UUID batchId) {

        UUID confirmedById = currentUser.id();
        log.info("Confirmando lote completo {} (usuario {})", batchId, confirmedById);

        DisbursementBatchSummaryDTOResponse batch = disbursementConfirmationService.confirmDisbursementBatch(
                batchId, confirmedById);

        return ResponseEntity.ok(ApiResponse.success(batch, "El lote de desembolso ha sido confirmado."));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmDisbursement(
            @Valid @RequestBody ConfirmDisbursementRequestDTO request) {

        UUID confirmedById = currentUser.id();
        log.info("Confirmando desembolso de {} documentos por usuario {}",
                request.documentIds().size(), confirmedById);

        disbursementConfirmationService.confirmDisbursement(request.documentIds(), confirmedById);

        return ResponseEntity.ok(ApiResponse.success(null, "El desembolso de los documentos ha sido confirmado."));
    }
}
