package com.davivienda.factoraje.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.PageResponse;
import com.davivienda.factoraje.dto.parameter.ParameterDTORequest;
import com.davivienda.factoraje.dto.parameter.ParameterDTOResponse;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.service.ParameterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/parameters")
@RequiredArgsConstructor
public class ParameterController {

    private final ParameterService parameterService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ParameterDTOResponse>>> readAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Reading all parameters");

        Pageable pageable = PageRequests.of(page, size, Sort.by("key"));
        PageResponse<ParameterDTOResponse> response = PageResponse.from(parameterService.readAllParameters(pageable));

        return ResponseEntity.ok(ApiResponse.success(response, "Parámetros obtenidos exitosamente."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ParameterDTOResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ParameterDTORequest request) {

        log.info("Iniciando la actualización de la entidad con ID: {}", id);
        ParameterDTOResponse response = parameterService.updateParameter(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Parámetro actualizado exitosamente."));
    }

}
