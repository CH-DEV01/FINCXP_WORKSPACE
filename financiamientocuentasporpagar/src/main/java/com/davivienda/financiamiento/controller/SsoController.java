package com.davivienda.financiamiento.controller;

import com.davivienda.financiamiento.domain.dto.StartSessionRequestDTO;
import com.davivienda.financiamiento.domain.dto.StartSessionResponseDTO;
import com.davivienda.financiamiento.domain.entities.SystemParameterModel;
import com.davivienda.financiamiento.repository.ParameterRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Slf4j
public class SsoController {

        @Autowired
        private RestTemplate restTemplate;

        private final ParameterRepository parameterRepository;

        public SsoController(ParameterRepository parameterRepository) {
                this.parameterRepository = parameterRepository;
                log.info("SsoController initialized");

        }

        @PostMapping("/start-session")
        @ResponseBody
        public String startSession(
                        @RequestParam("sessionToken") String sessionToken,
                        HttpServletRequest request,
                        HttpServletResponse httpResponse) {

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

                String jwt = response.getBody().getJwt();

                return "<html><body><script>" +
                                "localStorage.setItem('jwt_token', '" + jwt + "');" +
                                "window.location.href = '/financiamientocuentasporpagar/';" +
                                "</script></body></html>";

        }

}
