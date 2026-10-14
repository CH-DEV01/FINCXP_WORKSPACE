package com.davivienda.financiamiento.controller;

import com.davivienda.financiamiento.domain.dto.StartSessionRequestDTO;
import com.davivienda.financiamiento.domain.dto.StartSessionResponseDTO;
import com.davivienda.financiamiento.domain.entities.SystemParameterModel;
import com.davivienda.financiamiento.repository.ParameterRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Slf4j
public class SsoController {

        private static final String APP_HOME = "/financiamientocuentasporpagar/";

        // El JWT se escribe dentro de un <script>: solo se aceptan caracteres Base64URL y puntos.
        private static final Pattern JWT_FORMAT = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

        @Autowired
        private RestTemplate restTemplate;

        private final ParameterRepository parameterRepository;

        public SsoController(ParameterRepository parameterRepository) {
                this.parameterRepository = parameterRepository;
                log.info("SsoController initialized");

        }

        /** Canjea el sessionToken con la API y guarda el JWT en el navegador antes de abrir la aplicación. */
        @PostMapping(value = "/start-session", produces = MediaType.TEXT_HTML_VALUE)
        public String startSession(
                        @RequestParam("sessionToken") String sessionToken,
                        HttpServletRequest request) {

                log.info("[start-session] Solicitud recibida - IP: {}, sessionToken recibido: {}",
                                request.getRemoteAddr(), sessionToken != null ? "presente" : "ausente");

                SystemParameterModel uriParameter = parameterRepository.findByKey("FIN_CXP_URI")
                                .orElseThrow(() -> new RuntimeException("URI not found"));

                log.info("Uri for request: " + uriParameter.getValue());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                StartSessionRequestDTO body = new StartSessionRequestDTO();
                body.setSessionToken(sessionToken);

                HttpEntity<StartSessionRequestDTO> requestEntity = new HttpEntity<>(body, headers);

                ResponseEntity<StartSessionResponseDTO> response = restTemplate.exchange(
                                uriParameter.getValue(),
                                HttpMethod.POST,
                                requestEntity,
                                StartSessionResponseDTO.class);

                StartSessionResponseDTO result = response.getBody();
                String jwt = result != null ? result.getJwt() : null;
                log.info("[SSO-TRACE][start-session][{}] Respuesta de la API: status={}, success={}, message='{}', jwt={}",
                                sessionToken, response.getStatusCode(),
                                result != null ? result.getSuccess() : null,
                                result != null ? result.getMessage() : null, jwt);

                if (jwt == null || !JWT_FORMAT.matcher(jwt).matches()) {
                        // Sin token, el cliente web no encuentra sesión y vuelve al portal.
                        log.warn("[start-session] La API no activó la sesión: {}",
                                        result != null ? result.getMessage() : "sin respuesta");
                        return redirectPage("");
                }

                return redirectPage("localStorage.setItem('jwt_token', '" + jwt + "');");
        }

        private static String redirectPage(String script) {
                return "<html><body><script>" + script
                                + "window.location.replace('" + APP_HOME + "');"
                                + "</script></body></html>";
        }

}
