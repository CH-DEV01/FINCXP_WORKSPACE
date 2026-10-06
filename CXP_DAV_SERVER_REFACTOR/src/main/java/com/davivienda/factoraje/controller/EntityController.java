package com.davivienda.factoraje.controller;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
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
import com.davivienda.factoraje.dto.entity.EntityDTORequest;
import com.davivienda.factoraje.dto.entity.EntityDTOResponse;
import com.davivienda.factoraje.dto.entity.PayerRegistrationDTORequest;
import com.davivienda.factoraje.dto.entity.PayerSummaryDTOResponse;
import com.davivienda.factoraje.dto.entity.PayerUpdateDTORequest;
import com.davivienda.factoraje.dto.entity.SupplierSummaryDTOResponse;
import com.davivienda.factoraje.dto.entity.SupplierUpdateDTORequest;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.service.EntityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
public class EntityController {

    private static final Set<String> SORT_FIELDS = Set.of("name", "code", "nit", "status", "createdAt");

    private final EntityService entityService;


    @PostMapping
    public ResponseEntity<ApiResponse<EntityDTOResponse>> create(@Valid @RequestBody EntityDTORequest request) {

        log.info("Initiating the creation of a new entity");

        EntityDTOResponse response = entityService.createEntity(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response, "Entidad creada exitosamente."));

    }

    @PostMapping("/payers")
    public ResponseEntity<ApiResponse<PayerSummaryDTOResponse>> registerPayer(
            @Valid @RequestBody PayerRegistrationDTORequest request) {

        log.info("Registering a new payer with its credit facility and pricing term");

        PayerSummaryDTOResponse response = entityService.registerPayer(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/entities/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Pagador registrado correctamente."));
    }

    @PutMapping("/payers/{id}")
    public ResponseEntity<ApiResponse<PayerSummaryDTOResponse>> updatePayer(
            @PathVariable UUID id,
            @Valid @RequestBody PayerUpdateDTORequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                entityService.updatePayer(id, request),
                "Pagador actualizado correctamente."));
    }

    @GetMapping("/payers/summary")
    public ResponseEntity<ApiResponse<PageResponse<PayerSummaryDTOResponse>>> getPayerSummaries(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(entityService.getPayerSummaries(search, page, size)),
                "Resumen de pagadores obtenido exitosamente"));
    }

    @GetMapping("/suppliers/summary")
    public ResponseEntity<ApiResponse<PageResponse<SupplierSummaryDTOResponse>>> getSupplierSummaries(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(entityService.getSupplierSummaries(search, page, size)),
                "Resumen de proveedores obtenido exitosamente"));
    }

    @PutMapping("/suppliers/{id}")
    public ResponseEntity<ApiResponse<SupplierSummaryDTOResponse>> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody SupplierUpdateDTORequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                entityService.updateSupplier(id, request),
                "Proveedor actualizado correctamente."));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<PageResponse<EntityDTOResponse>>> getEntities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        Pageable pageable = createPageRequest(page, size, sortBy, sortDir);
        PageResponse<EntityDTOResponse> entities = PageResponse.from(entityService.getEntities(pageable));

        return ResponseEntity.ok(ApiResponse.success(entities, "Catálogo de entidades obtenido exitosamente"));
    }

    @GetMapping("/for-role/{roleId}")
    public ResponseEntity<ApiResponse<PageResponse<EntityDTOResponse>>> searchEntitiesForRole(
            @PathVariable UUID roleId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                PageResponse.from(entityService.searchEntitiesForRole(roleId, search, page, size)),
                "Entidades asignables al rol obtenidas exitosamente"));
    }

    @GetMapping("/payers")
    public ResponseEntity<ApiResponse<PageResponse<EntityDTOResponse>>> getPayers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        Pageable pageable = createPageRequest(page, size, sortBy, sortDir);
        PageResponse<EntityDTOResponse> payers = PageResponse.from(entityService.getPayers(pageable));

        return ResponseEntity.ok(ApiResponse.success(payers, "Catálogo de pagadores obtenido exitosamente"));
    }

    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponse<PageResponse<EntityDTOResponse>>> getSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        Pageable pageable = createPageRequest(page, size, sortBy, sortDir);
        PageResponse<EntityDTOResponse> suppliers = PageResponse.from(entityService.getSuppliers(pageable));

        return ResponseEntity.ok(ApiResponse.success(suppliers, "Catálogo de proveedores obtenido exitosamente"));
    }

    private Pageable createPageRequest(int page, int size, String sortBy, String sortDir) {
        return PageRequests.of(page, size, sortBy, sortDir, SORT_FIELDS);
    }

}
