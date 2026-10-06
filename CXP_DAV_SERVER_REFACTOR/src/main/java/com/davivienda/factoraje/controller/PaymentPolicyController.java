package com.davivienda.factoraje.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.payment_policy.PaymentPolicyDTOResponse;
import com.davivienda.factoraje.service.PaymentPolicyCatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment-policies")
@RequiredArgsConstructor
public class PaymentPolicyController {

    private final PaymentPolicyCatService paymentPolicyCatService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentPolicyDTOResponse>>> getPaymentPolicies() {

        log.info("Consultando catálogo de políticas de pago");

        List<PaymentPolicyDTOResponse> paymentPolicies = paymentPolicyCatService.getPaymentPolicies();

        return ResponseEntity.ok(ApiResponse.success(paymentPolicies,"Catálogo de políticas de pago obtenido correctamente."));
    }

}
