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
            @RequestBody HandoffDTORequest request,
            HttpServletRequest httpRequest) {

        log.info("Endpoint /handoff activated");
        log.info("[SSO-TRACE][handoff] Petición: {} {} desde ip={}, X-Forwarded-For='{}', User-Agent='{}', "
                + "app='{}', otc='{}', Authorization='{}'",
                httpRequest.getMethod(), httpRequest.getRequestURL(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("X-Forwarded-For"), httpRequest.getHeader("User-Agent"),
                request.getApp(), request.getOtc(), bearerTokenRequest);

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
        log.info("[SSO-TRACE][activate] Petición: {} {} desde ip={}, X-Forwarded-For='{}', contextPath='{}', "
                + "sessionToken='{}'",
                httpRequest.getMethod(), httpRequest.getRequestURL(), httpRequest.getRemoteAddr(),
                httpRequest.getHeader("X-Forwarded-For"), httpRequest.getContextPath(), request.getSessionToken());

        try {
            String jwt = ssoService.activateSession(request.getSessionToken());
            log.info("JWT generado correctamente");

            McsSSODTOResponse response = new McsSSODTOResponse();
            response.setMessage("Sesión activada correctamente");
            response.setSuccess(true);

            String cookie = sessionCookies.create(jwt, httpRequest).toString();
            log.info("[SSO-TRACE][activate][{}] Set-Cookie enviado al portal: {}", request.getSessionToken(), cookie);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie)
                    .body(response);

        } catch (RuntimeException e) {

            log.error("Error al activar sesión del usuario: {}", e.getMessage());
            log.error("[SSO-TRACE][activate][{}] FIN con error: {}: {}",
                    request.getSessionToken(), e.getClass().getName(), e.getMessage());

            McsSSODTOResponse errorResponse = new McsSSODTOResponse();
            errorResponse.setJwt(null);
            errorResponse.setMessage("Error al activar la sesión"); 
            errorResponse.setSuccess(false);

            return ResponseEntity.ok(errorResponse);
        }
    }

}
