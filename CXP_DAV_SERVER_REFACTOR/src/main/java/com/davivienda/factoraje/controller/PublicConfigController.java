package com.davivienda.factoraje.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.PublicConfigDTO;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;

import lombok.RequiredArgsConstructor;

/** Público: el cliente lo consulta antes de tener sesión y después de perderla. */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicConfigController {

    private final SystemParameters systemParameters;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PublicConfigDTO>> getConfig() {
        String loginUrl = systemParameters.get(SystemParameterKey.APP_LOGIN_URL);
        String portalUrl = loginUrl == null || loginUrl.isBlank() || SystemParameterKey.UNCONFIGURED.equals(loginUrl)
                ? null
                : loginUrl;
        return ResponseEntity.ok(ApiResponse.success(new PublicConfigDTO(portalUrl), "Configuración obtenida."));
    }
}
