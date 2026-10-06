package com.davivienda.factoraje.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.sso.HandoffDTORequest;
import com.davivienda.factoraje.dto.sso.HandoffDTOResponse;
import com.davivienda.factoraje.dto.sso.McsSSODTOResponse;
import com.davivienda.factoraje.dto.sso.SessionTokenDTORequest;
import com.davivienda.factoraje.service.SsoService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/sso")
@Slf4j
public class SsoController {

    private final SsoService ssoService;

    public SsoController(SsoService ssoService) {

        this.ssoService = ssoService;
        log.info("SsoService initialized");

    }

    @PostMapping("/handoff")
    public ResponseEntity<HandoffDTOResponse> handoff(
            @RequestHeader("Authorization") String bearerTokenRequest,
            @RequestBody HandoffDTORequest request) {

        log.info("Endpoint /handoff activated");
        log.info("Bearer Token received: {}", bearerTokenRequest);

        HandoffDTOResponse response = ssoService.processHandoff(request.getApp(), request.getOtc(), bearerTokenRequest);

        log.info("response to return: {}", response);
        return ResponseEntity.ok(response);

    }

    @PostMapping("/activateSession")
    public ResponseEntity<McsSSODTOResponse> activateSession(
            @RequestBody SessionTokenDTORequest request) {

        log.info("Endpoint /activateSession activated");
        log.info("sessionToken received: {}", request.getSessionToken());

        try {
            String jwt = ssoService.activateSession(request.getSessionToken());
            log.info("JWT generado correctamente: {}", jwt);

            McsSSODTOResponse response = new McsSSODTOResponse();
            response.setJwt(jwt);
            response.setMessage("Sesión activada correctamente");
            response.setSuccess(true);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            log.error("Error al activar sesión del usuario: {}", e.getMessage());

            McsSSODTOResponse errorResponse = new McsSSODTOResponse();
            errorResponse.setJwt(null);
            errorResponse.setMessage("Error al activar la sesión"); 
            errorResponse.setSuccess(false);

            return ResponseEntity.ok(errorResponse);
        }
    }

}
