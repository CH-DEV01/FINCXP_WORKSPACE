package com.davivienda.factoraje.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.supplier.SupplierBankAccountDTOResponse;
import com.davivienda.factoraje.service.SupplierBankAccountService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierBankAccountService supplierBankAccountService;

    @GetMapping("/me/bank-account")
    public ResponseEntity<ApiResponse<SupplierBankAccountDTOResponse>> getOwnBankAccount() {
        return ResponseEntity.ok(ApiResponse.success(
                supplierBankAccountService.getOwnMainAccount(),
                "Cuenta bancaria del proveedor obtenida exitosamente."));
    }
}
