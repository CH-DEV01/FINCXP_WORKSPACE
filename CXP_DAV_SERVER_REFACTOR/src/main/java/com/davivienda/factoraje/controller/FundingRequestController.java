package com.davivienda.factoraje.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.funding_request.CalculateDTORequest;
import com.davivienda.factoraje.dto.funding_request.CalculateDTOResponse;
import com.davivienda.factoraje.dto.funding_request.SubmitFundingDTORequest;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.service.FundingRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/funding-requests")
public class FundingRequestController {

    private final FundingRequestService fundingRequestService;
    private final CurrentUserService currentUser;

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<CalculateDTOResponse>> calculateCost(
            @Valid @RequestBody CalculateDTORequest request) {

        currentUser.requireOwnMasterAgreement(request.masterAgreementId());

        CalculateDTOResponse response = fundingRequestService.checkCost(
                request.masterAgreementId(),
                request.documentIds());

        return ResponseEntity.ok(ApiResponse.success(response, "Cálculo realizado con éxito"));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<CalculateDTOResponse>> submitFunding(
            @Valid @RequestBody SubmitFundingDTORequest request,
            @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent) {

        currentUser.requireOwnMasterAgreement(request.masterAgreementId());

        CalculateDTOResponse response = fundingRequestService.submitFundingRequest(
                request.masterAgreementId(),
                currentUser.id(),
                request.documentIds(),
                request.termVersionId(),
                userAgent);

        return ResponseEntity.ok(ApiResponse.success(response, "Solicitud de desembolso procesada con éxito"));
    }

}
