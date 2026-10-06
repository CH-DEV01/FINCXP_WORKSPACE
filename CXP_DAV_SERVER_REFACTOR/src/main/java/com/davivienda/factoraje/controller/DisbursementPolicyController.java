package com.davivienda.factoraje.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.disbursement_policy.DisbursementPolicyDTOResponse;
import com.davivienda.factoraje.service.DisbursementPolicyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/disbursement-policies")
@RequiredArgsConstructor
public class DisbursementPolicyController {

    private final DisbursementPolicyService disbursementPolicyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DisbursementPolicyDTOResponse>>> getDisbursementPolicies() {

        log.info("Consultando catálogo de políticas de desembolso");

        List<DisbursementPolicyDTOResponse> disbursementPolicies = disbursementPolicyService.getDisbursementPolicies();

        return ResponseEntity
                .ok(ApiResponse.success(disbursementPolicies, "Catálogo de políticas de desembolso obtenido correctamente."));
    }
}
