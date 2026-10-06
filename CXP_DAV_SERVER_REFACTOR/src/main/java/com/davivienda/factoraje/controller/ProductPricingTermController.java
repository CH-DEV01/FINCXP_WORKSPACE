package com.davivienda.factoraje.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.product_pricing_term.ProductPricingTermDTORequest;
import com.davivienda.factoraje.dto.product_pricing_term.ProductPricingTermDTOResponse;
import com.davivienda.factoraje.service.ProductPricingTermService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/product-pricing-terms")
@RequiredArgsConstructor
public class ProductPricingTermController {

    private final ProductPricingTermService productPricingTermService;


    @PostMapping
    public ResponseEntity<ApiResponse<ProductPricingTermDTOResponse>> create(
            @Valid @RequestBody ProductPricingTermDTORequest request) {

        log.info("Initiating the creation of a new product pricing term");

        ProductPricingTermDTOResponse response = productPricingTermService.createProductPricingTerm(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Tarifario creado exitosamente."));

    }

}
