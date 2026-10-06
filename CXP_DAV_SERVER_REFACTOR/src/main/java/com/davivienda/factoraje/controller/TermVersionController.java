package com.davivienda.factoraje.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.domain.enums.TermTypeUniqueCodeEnum;
import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.term_version.TermTypeDTOResponse;
import com.davivienda.factoraje.dto.term_version.TermVersionAdminDTOResponse;
import com.davivienda.factoraje.dto.term_version.TermVersionDTORequest;
import com.davivienda.factoraje.dto.term_version.TermVersionDTOResponse;
import com.davivienda.factoraje.service.TermVersionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Las rutas /active/** son de lectura para cualquier usuario autenticado; el resto es
 * administración y SecurityConfig la restringe por rol.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/term-versions")
public class TermVersionController {

    private final TermVersionService termVersionService;

    @GetMapping("/active/supplier")
    public ResponseEntity<ApiResponse<TermVersionDTOResponse>> getActiveSupplierTerm() {
        return ResponseEntity.ok(ApiResponse.success(
                termVersionService.getActive(TermTypeUniqueCodeEnum.SUPPLIER_TERM_TYPE),
                "Versión activa de términos del proveedor obtenida con éxito"));
    }

    @GetMapping("/active/payer")
    public ResponseEntity<ApiResponse<TermVersionDTOResponse>> getActivePayerTerm() {
        return ResponseEntity.ok(ApiResponse.success(
                termVersionService.getActive(TermTypeUniqueCodeEnum.PAYER_TERM_TYPE),
                "Versión activa de términos del pagador obtenida con éxito"));
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<TermTypeDTOResponse>>> getTermTypes() {
        return ResponseEntity.ok(ApiResponse.success(
                termVersionService.getTermTypes(), "Tipos de término obtenidos con éxito"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TermVersionAdminDTOResponse>>> getVersions(
            @RequestParam("termTypeCode") String termTypeCode) {
        return ResponseEntity.ok(ApiResponse.success(
                termVersionService.getVersions(termTypeCode), "Versiones obtenidas con éxito"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TermVersionAdminDTOResponse>> getVersion(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                termVersionService.getVersion(id), "Versión obtenida con éxito"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TermVersionAdminDTOResponse>> createDraft(
            @Valid @RequestBody TermVersionDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                termVersionService.createDraft(request), "Borrador creado con éxito"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TermVersionAdminDTOResponse>> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody TermVersionDTORequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                termVersionService.updateDraft(id, request), "Borrador actualizado con éxito"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID id) {
        termVersionService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<TermVersionAdminDTOResponse>> publish(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                termVersionService.publish(id), "Versión publicada con éxito"));
    }
}
