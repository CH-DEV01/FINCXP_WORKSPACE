package com.davivienda.factoraje.controller;

import org.springframework.http.HttpHeaders;
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
import com.davivienda.factoraje.infrastructure.security.SessionCookies;
import com.davivienda.factoraje.service.SsoService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/sso")
@Slf4j
public class SsoController {

    private final SsoService ssoService;
    private final SessionCookies sessionCookies;

    public SsoController(SsoService ssoService, SessionCookies sessionCookies) {

        this.ssoService = ssoService;
        this.sessionCookies = sessionCookies;
        log.info("SsoService initialized");

    }

    @PostMapping("/handoff")
    public ResponseEntity<HandoffDTOResponse> handoff(
            @RequestHeader("Authorization") String bearerTokenRequest,
            @RequestBody HandoffDTORequest request) {

        log.info("Endpoint /handoff activated");

        HandoffDTOResponse response = ssoService.processHandoff(request.getApp(), request.getOtc(), bearerTokenRequest);

        log.info("Handoff respondido. success: {}", response.getSuccess());
        return ResponseEntity.ok(response);

    }

    /**
     * Lo llama el portal, que reenvía al navegador el Set-Cookie con el JWT. El token
     * no viaja en el cuerpo.
     */
    @PostMapping("/activateSession")
    public ResponseEntity<McsSSODTOResponse> activateSession(
            @RequestBody SessionTokenDTORequest request,
            HttpServletRequest httpRequest) {

        log.info("Endpoint /activateSession activated");

        try {
            String jwt = ssoService.activateSession(request.getSessionToken());
            log.info("JWT generado correctamente");

            McsSSODTOResponse response = new McsSSODTOResponse();
            response.setMessage("Sesión activada correctamente");
            response.setSuccess(true);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, sessionCookies.create(jwt, httpRequest).toString())
                    .body(response);

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
