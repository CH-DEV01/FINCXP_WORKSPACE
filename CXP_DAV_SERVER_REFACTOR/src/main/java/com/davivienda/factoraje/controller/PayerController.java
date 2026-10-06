package com.davivienda.factoraje.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.payer.PayerSummaryDTOResponse;
import com.davivienda.factoraje.service.PayerSummaryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payers")
@RequiredArgsConstructor
public class PayerController {

    private final PayerSummaryService payerSummaryService;

    @GetMapping("/me/summary")
    public ResponseEntity<ApiResponse<PayerSummaryDTOResponse>> getOwnSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                payerSummaryService.getOwnSummary(),
                "Resumen del pagador obtenido exitosamente."));
    }

    @GetMapping("/{payerId}/summary")
    public ResponseEntity<ApiResponse<PayerSummaryDTOResponse>> getSummary(@PathVariable UUID payerId) {
        return ResponseEntity.ok(ApiResponse.success(
                payerSummaryService.getSummary(payerId),
                "Resumen del pagador obtenido exitosamente."));
    }
}
