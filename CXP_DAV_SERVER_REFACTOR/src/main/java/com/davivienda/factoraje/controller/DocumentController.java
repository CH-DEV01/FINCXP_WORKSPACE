package com.davivienda.factoraje.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.PageResponse;
import com.davivienda.factoraje.dto.document.DocumentDTOResponse;
import com.davivienda.factoraje.dto.document.DocumentHistoryDTOResponse;
import com.davivienda.factoraje.dto.document.DocumentHistorySummaryDTOResponse;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.service.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserService currentUser;


    @GetMapping("/master-agreement/{masterAgreementId}/financeable")
    public ResponseEntity<ApiResponse<List<DocumentDTOResponse>>> getFinanceableByMasterAgreement(
            @PathVariable UUID masterAgreementId) {

        log.info("Consultando documentos financiables para el convenio marco con ID: {}", masterAgreementId);
        currentUser.requireOwnMasterAgreement(masterAgreementId);

        List<DocumentDTOResponse> response = documentService
                .getFinanceableDocumentsByMasterAgreement(masterAgreementId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Documentos financiables obtenidos exitosamente."));
    }

    @GetMapping("/supplier/{supplierId}/history")
    public ResponseEntity<ApiResponse<PageResponse<DocumentHistoryDTOResponse>>> getHistoryBySupplier(
            @PathVariable UUID supplierId,
            @RequestParam(required = false) UUID payerId,
            @RequestParam(required = false) DocumentStatusEnum status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Consultando bitácora de documentos para el proveedor con ID: {}", supplierId);
        currentUser.requireOwnEntity(supplierId);

        PageResponse<DocumentHistoryDTOResponse> response = PageResponse.from(documentService
                .getDocumentHistoryBySupplier(supplierId, payerId, status, search, page, size));

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Bitácora de documentos obtenida exitosamente."));
    }

    @GetMapping("/payer/{payerId}/history")
    public ResponseEntity<ApiResponse<PageResponse<DocumentHistoryDTOResponse>>> getHistoryByPayer(
            @PathVariable UUID payerId,
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) DocumentStatusEnum status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Consultando bitácora de documentos para el pagador con ID: {}", payerId);
        currentUser.requireOwnEntity(payerId);

        PageResponse<DocumentHistoryDTOResponse> response = PageResponse.from(documentService
                .getDocumentHistoryByPayer(payerId, supplierId, status, search, page, size));

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Bitácora de documentos obtenida exitosamente."));
    }

    @GetMapping("/payer/{payerId}/history/summary")
    public ResponseEntity<ApiResponse<List<DocumentHistorySummaryDTOResponse>>> getHistorySummaryByPayer(
            @PathVariable UUID payerId) {

        currentUser.requireOwnEntity(payerId);

        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocumentHistorySummaryByPayer(payerId),
                "Resumen de la bitácora obtenido exitosamente."));
    }

    @PatchMapping("/{documentId}/inactivate")
    public ResponseEntity<ApiResponse<Void>> inactivateDocument(@PathVariable UUID documentId) {

        log.info("Inactivación manual del documento con ID: {}", documentId);

        documentService.inactivateDocumentByPayer(documentId);

        return ResponseEntity.ok(ApiResponse.success(null, "El documento pasó a estado Inactivo."));
    }

}
