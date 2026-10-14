package com.davivienda.financiamiento.controller;

import com.davivienda.financiamiento.domain.dto.StartSessionRequestDTO;
import com.davivienda.financiamiento.domain.dto.StartSessionResponseDTO;
import com.davivienda.financiamiento.domain.entities.SystemParameterModel;
import com.davivienda.financiamiento.repository.ParameterRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Slf4j
public class SsoController {

        private static final URI APP_HOME = URI.create("/financiamientocuentasporpagar/");

        @Autowired
        private RestTemplate restTemplate;

        private final ParameterRepository parameterRepository;

        public SsoController(ParameterRepository parameterRepository) {
                this.parameterRepository = parameterRepository;
                log.info("SsoController initialized");

        }

        /**
         * Canjea el sessionToken con la API y reenvía al navegador la cookie HttpOnly
         * que la API arma con el JWT. El token no pasa por JavaScript.
         */
        @PostMapping("/start-session")
        public ResponseEntity<Void> startSession(
                        @RequestParam("sessionToken") String sessionToken,
                        HttpServletRequest request) {

                log.info("[start-session] Solicitud recibida - IP: {}, sessionToken recibido: {}",
                                request.getRemoteAddr(), sessionToken != null ? "presente" : "ausente");

                SystemParameterModel uriParameter = parameterRepository.findByKey("FIN_CXP_URI")
                                .orElseThrow(() -> new RuntimeException("URI not found"));

                log.info("Uri for request: " + uriParameter.getValue());
                log.info("[SSO-TRACE][start-session][{}] 1. Petición: {} {}?{} desde ip={}, X-Forwarded-For='{}', "
                                + "Referer='{}', User-Agent='{}'",
                                sessionToken, request.getMethod(), request.getRequestURL(), request.getQueryString(),
                                request.getRemoteAddr(), request.getHeader("X-Forwarded-For"),
                                request.getHeader("Referer"), request.getHeader("User-Agent"));
                log.info("[SSO-TRACE][start-session][{}] 2. Parámetro FIN_CXP_URI(id={})='{}'",
                                sessionToken, uriParameter.getId(), uriParameter.getValue());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                StartSessionRequestDTO body = new StartSessionRequestDTO();
                body.setSessionToken(sessionToken);

                HttpEntity<StartSessionRequestDTO> requestEntity = new HttpEntity<>(body, headers);
                log.info("[SSO-TRACE][start-session][{}] 3. Petición a la API: POST {} headers={} body={{\"sessionToken\":\"{}\"}}",
                                sessionToken, uriParameter.getValue(), headers, sessionToken);

                long started = System.currentTimeMillis();
                ResponseEntity<StartSessionResponseDTO> response;
                try {
                        response = restTemplate.exchange(
                                        uriParameter.getValue(),
                                        HttpMethod.POST,
                                        requestEntity,
                                        StartSessionResponseDTO.class);
                } catch (RuntimeException e) {
                        log.error("[SSO-TRACE][start-session][{}] FIN: falló la llamada a la API en {} ms: {}: {}",
                                        sessionToken, System.currentTimeMillis() - started,
                                        e.getClass().getName(), e.getMessage());
                        throw e;
                }

                log.info("[SSO-TRACE][start-session][{}] 4. Respuesta de la API en {} ms: status={} headers={} "
                                + "success={} message='{}'",
                                sessionToken, System.currentTimeMillis() - started, response.getStatusCode(),
                                response.getHeaders(),
                                response.getBody() != null ? response.getBody().getSuccess() : null,
                                response.getBody() != null ? response.getBody().getMessage() : null);

                HttpHeaders redirect = new HttpHeaders();
                redirect.setLocation(APP_HOME);

                List<String> cookies = response.getHeaders().getOrEmpty(HttpHeaders.SET_COOKIE);
                if (cookies.isEmpty()) {
                        // Sin cookie, el cliente web no encuentra sesión y vuelve al portal.
                        log.warn("[start-session] La API no activó la sesión: {}",
                                        response.getBody() != null ? response.getBody().getMessage() : "sin respuesta");
                } else {
                        redirect.put(HttpHeaders.SET_COOKIE, cookies);
                }

                log.info("[SSO-TRACE][start-session][{}] 5. Respuesta al navegador: 302 Location={} Set-Cookie={}",
                                sessionToken, APP_HOME, cookies);
                return new ResponseEntity<>(redirect, HttpStatus.FOUND);

        }

}
