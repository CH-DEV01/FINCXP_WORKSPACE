package com.davivienda.factoraje.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.PageResponse;
import com.davivienda.factoraje.dto.credit_facility.CreditFacilityDTOResponse;
import com.davivienda.factoraje.dto.credit_facility.RestoreCreditFacilityDTORequest;
import com.davivienda.factoraje.dto.credit_facility_history.CreditFacilityHistoryDTOResponse;
import com.davivienda.factoraje.service.CreditFacilityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/credit-facilities")
@RequiredArgsConstructor
public class CreditFacilityController {

    private final CreditFacilityService creditFacilityService;


    @GetMapping("/payer/{payerId}")
    public ResponseEntity<ApiResponse<CreditFacilityDTOResponse>> getByPayer(@PathVariable UUID payerId) {

        log.info("Consultando cupo de crédito para el pagador con ID: {}", payerId);
        CreditFacilityDTOResponse response = creditFacilityService.findByPayerId(payerId);

        return ResponseEntity.ok(ApiResponse.success(response, "Cupo de crédito consultado exitosamente."));
    }

    @PostMapping("/restore")
    public ResponseEntity<ApiResponse<CreditFacilityDTOResponse>> restoreCreditFacility(
            @Valid @RequestBody RestoreCreditFacilityDTORequest request,
            Authentication authentication) {

        String loggedUser = authentication.getName();

        CreditFacilityDTOResponse updatedFacility = creditFacilityService.restoreCreditFacility(request, loggedUser);

        return ResponseEntity.ok(ApiResponse.success(
                updatedFacility,
                "Abono aplicado y cupo restaurado exitosamente."));
    }

    @GetMapping("/history/{creditFacilityId}")
    public ResponseEntity<ApiResponse<PageResponse<CreditFacilityHistoryDTOResponse>>> getHistory(
            @PathVariable UUID creditFacilityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Consultando historial de abonos para la facilidad de crédito con ID: {}", creditFacilityId);

        PageResponse<CreditFacilityHistoryDTOResponse> history = PageResponse.from(
                creditFacilityService.getHistory(creditFacilityId, page, size));

        return ResponseEntity.ok(ApiResponse.success(
                history,
                "Historial de abonos consultado exitosamente."));
    }

}
