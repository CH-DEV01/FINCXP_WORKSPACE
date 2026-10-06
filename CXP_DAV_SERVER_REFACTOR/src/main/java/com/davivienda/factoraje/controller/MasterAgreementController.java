package com.davivienda.factoraje.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.PageResponse;
import com.davivienda.factoraje.dto.disbursement_policy.NextDisbursementDateDTOResponse;
import com.davivienda.factoraje.dto.master_agreement.MasterAgreementDTORequest;
import com.davivienda.factoraje.dto.master_agreement.MasterAgreementDTOResponse;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.service.MasterAgreementService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/master-agreements")
@RequiredArgsConstructor
public class MasterAgreementController {

    private final MasterAgreementService masterAgreementService;
    private final CurrentUserService currentUser;


    @PostMapping
    public ResponseEntity<ApiResponse<MasterAgreementDTOResponse>> create(
            @Valid @RequestBody MasterAgreementDTORequest request) {

        log.info("Initiating the creation of a new master agreement");

        MasterAgreementDTOResponse response = masterAgreementService.createMasterAgreement(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Convenio marco creado exitosamente."));

    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MasterAgreementDTOResponse>>> getPaginatedAgreements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID payerId) {
        PageResponse<MasterAgreementDTOResponse> paginatedResult = PageResponse.from(
                masterAgreementService.getPaginatedAgreements(page, size, search, payerId));

        return ResponseEntity.ok(ApiResponse.success(paginatedResult, "Convenios obtenidos exitosamente"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MasterAgreementDTOResponse>> updateMasterAgreement(
            @PathVariable UUID id,
            @Valid @RequestBody MasterAgreementDTORequest request) {

        log.info("Recibida petición para actualizar el convenio marco con ID: {}", id);

        MasterAgreementDTOResponse response = masterAgreementService.updateMasterAgreement(id, request);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Convenio marco actualizado exitosamente."));
    }

    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<ApiResponse<List<MasterAgreementDTOResponse>>> getBySupplier(
            @PathVariable UUID supplierId) {

        log.info("Consultando convenios marco para el proveedor con ID: {}", supplierId);
        currentUser.requireOwnEntity(supplierId);

        List<MasterAgreementDTOResponse> response = masterAgreementService
                .getMasterAgreementsBySupplier(supplierId, !currentUser.isAdmin());

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Convenios marco del proveedor obtenidos exitosamente."));
    }

    @GetMapping("/{id}/next-disbursement-date")
    public ResponseEntity<ApiResponse<NextDisbursementDateDTOResponse>> getNextDisbursementDate(
            @PathVariable UUID id) {

        log.info("Consultando próxima fecha de desembolso para el convenio marco con ID: {}", id);
        currentUser.requireOwnMasterAgreement(id);

        NextDisbursementDateDTOResponse response = masterAgreementService.getNextDisbursementDate(id);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Próxima fecha de desembolso calculada exitosamente."));
    }

}
