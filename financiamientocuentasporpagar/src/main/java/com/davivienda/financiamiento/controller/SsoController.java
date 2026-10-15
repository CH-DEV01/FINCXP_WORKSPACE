package com.davivienda.financiamiento.controller;

import com.davivienda.financiamiento.domain.dto.StartSessionRequestDTO;
import com.davivienda.financiamiento.domain.dto.StartSessionResponseDTO;
import com.davivienda.financiamiento.domain.entities.SystemParameterModel;
import com.davivienda.financiamiento.repository.ParameterRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Slf4j
public class SsoController {

        private static final String APP_HOME = "/financiamientocuentasporpagar/";

        private static final String DEFAULT_COOKIE_NAME = "fincxp_token";
        private static final Pattern COOKIE_NAME_FORMAT = Pattern.compile("[A-Za-z0-9_-]{1,64}");
        private static final String DEFAULT_COOKIE_DOMAIN = "davivienda.com.sv";
        private static final int DEFAULT_SESSION_MINUTES = 1440;
        private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1");

        // El JWT se escribe dentro de un <script>: solo se aceptan caracteres Base64URL
        // y puntos.
        private static final Pattern JWT_FORMAT = Pattern.compile("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

        @Autowired
        private RestTemplate restTemplate;

        private final ParameterRepository parameterRepository;

        public SsoController(ParameterRepository parameterRepository) {
                this.parameterRepository = parameterRepository;
                log.info("SsoController initialized");

        }

        /**
         * Canjea el sessionToken con la API y guarda el JWT en el navegador antes de
         * abrir la aplicación.
         */
        // @PostMapping(value = "/start-session", produces = MediaType.TEXT_HTML_VALUE)
        // public String startSession(
        //                 @RequestParam("sessionToken") String sessionToken,
        //                 HttpServletRequest request) {

        //         log.info("[start-session] Solicitud recibida - IP: {}, sessionToken recibido: {}",
        //                         request.getRemoteAddr(), sessionToken != null ? "presente" : "ausente");

        //         SystemParameterModel uriParameter = parameterRepository.findByKey("FIN_CXP_URI")
        //                         .orElseThrow(() -> new RuntimeException("URI not found"));

        //         log.info("Uri for request: " + uriParameter.getValue());

        //         HttpHeaders headers = new HttpHeaders();
        //         headers.setContentType(MediaType.APPLICATION_JSON);

        //         StartSessionRequestDTO body = new StartSessionRequestDTO();
        //         body.setSessionToken(sessionToken);

        //         HttpEntity<StartSessionRequestDTO> requestEntity = new HttpEntity<>(body, headers);

        //         ResponseEntity<StartSessionResponseDTO> response = restTemplate.exchange(
        //                         uriParameter.getValue(),
        //                         HttpMethod.POST,
        //                         requestEntity,
        //                         StartSessionResponseDTO.class);

        //         StartSessionResponseDTO result = response.getBody();
        //         String jwt = result != null ? result.getJwt() : null;
        //         log.info("[SSO-TRACE][start-session][{}] Respuesta de la API: status={}, success={}, message='{}', jwt={}",
        //                         sessionToken, response.getStatusCode(),
        //                         result != null ? result.getSuccess() : null,
        //                         result != null ? result.getMessage() : null, jwt);

        //         if (jwt == null || !JWT_FORMAT.matcher(jwt).matches()) {
        //                 // Sin token, el cliente web no encuentra sesión y vuelve al portal.
        //                 log.warn("[start-session] La API no activó la sesión: {}",
        //                                 result != null ? result.getMessage() : "sin respuesta");
        //                 return redirectPage("");
        //         }

        //         return redirectPage("localStorage.setItem('jwt_token', '" + jwt + "');");
        // }

        // private static String redirectPage(String script) {
        //         return "<html><body><script>" + script
        //                         + "window.location.replace('" + APP_HOME + "');"
        //                         + "</script></body></html>";
        // }

        @PostMapping("/start-session")
        public RedirectView startSession(
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

                StartSessionResponseDTO result = response.getBody();
                String jwt = result != null ? result.getJwt() : null;

                if (result != null && Boolean.TRUE.equals(result.getSuccess())
                                && jwt != null && JWT_FORMAT.matcher(jwt).matches()) {
                        addSessionCookie(request, httpResponse, jwt,
                                        Duration.ofMinutes(parameterInt("JWT_EXPIRATION_MINUTES", DEFAULT_SESSION_MINUTES)));
                } else {
                        // Sin cookie, el cliente web no encuentra sesión y vuelve al portal.
                        log.warn("[start-session] La API no activó la sesión: {}",
                                        result != null ? result.getMessage() : "sin respuesta");
                        addSessionCookie(request, httpResponse, "", Duration.ZERO);
                }
                return new RedirectView(APP_HOME);
        }

        /**
         * La API lee, renueva y borra esta misma cookie: nombre, dominio y ruta deben
         * coincidir con SessionCookies de la API.
         */
        private void addSessionCookie(HttpServletRequest request, HttpServletResponse httpResponse,
                        String value, Duration maxAge) {
                String host = request.getServerName().toLowerCase(Locale.ROOT);
                boolean local = LOCAL_HOSTS.contains(host);
                ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(cookieName(), value)
                                .path("/")
                                .httpOnly(true)
                                .secure(!local)
                                .sameSite("Lax")
                                .maxAge(maxAge);
                // El navegador rechaza una cookie para un dominio que no contiene al host.
                String domain = cookieDomain();
                if (host.equals(domain) || host.endsWith("." + domain)) {
                        cookie.domain(domain);
                }
                httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
        }

        private String cookieName() {
                return parameterRepository.findByKey("SESSION_COOKIE_NAME")
                                .map(SystemParameterModel::getValue)
                                .map(String::trim)
                                .filter(value -> COOKIE_NAME_FORMAT.matcher(value).matches())
                                .orElse(DEFAULT_COOKIE_NAME);
        }

        private String cookieDomain() {
                String domain = parameterRepository.findByKey("SESSION_COOKIE_DOMAIN")
                                .map(SystemParameterModel::getValue)
                                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                                .filter(value -> !value.isEmpty())
                                .orElse(DEFAULT_COOKIE_DOMAIN);
                return domain.startsWith(".") ? domain.substring(1) : domain;
        }

        private int parameterInt(String key, int defaultValue) {
                return parameterRepository.findByKey(key)
                                .map(SystemParameterModel::getValue)
                                .map(String::trim)
                                .filter(value -> value.matches("\\d{1,6}"))
                                .map(Integer::parseInt)
                                .orElse(defaultValue);
        }

}
