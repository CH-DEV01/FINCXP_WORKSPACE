package com.davivienda.factoraje.service.impl;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.davivienda.factoraje.domain.entities.PendingSessionModel;
import com.davivienda.factoraje.domain.entities.SystemParameterModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.sso.DataSession;
import com.davivienda.factoraje.dto.sso.HandoffDTOResponse;
import com.davivienda.factoraje.dto.sso.OtcDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.BusinessException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.JwtService;
import com.davivienda.factoraje.infrastructure.util.IdentifierNormalizer;
import com.davivienda.factoraje.repository.ParameterRepository;
import com.davivienda.factoraje.repository.PendingSessionRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.SsoService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SsoServiceImpl implements SsoService {

    private final ParameterRepository parameterRepository;
    private final PendingSessionRepository pendingSessionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final JwtService jwtService;

    public SsoServiceImpl(
            ParameterRepository parameterRepository,
            RestTemplate restTemplate,
            PendingSessionRepository pendingSessionRepository,
            UserRepository userRepository,
            JwtService jwtService) {
        this.parameterRepository = parameterRepository;
        this.restTemplate = restTemplate;
        this.pendingSessionRepository = pendingSessionRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        log.info("SsoServiceImpl initialized");
    }

    @Override
    public HandoffDTOResponse processHandoff(String appCode, String otc, String bearerTokenRequest) {

        log.info("[Handoff-Start] Iniciando proceso para AppCode: {} con OTC: {}", appCode, otc);

        try {
            // 1. CARGA DE PARÁMETROS (Configuración)
            SystemParameterModel myBearerToken = parameterRepository.findByKey("BEARER_TOKEN")
                    .orElseThrow(() -> new RuntimeException("Bearer token not found"));

            SystemParameterModel payBearerToken = parameterRepository.findByKey("PAY_BEARER_TOKEN")
                    .orElseThrow(() -> new RuntimeException("Pay bearer token not found"));

            SystemParameterModel myAppCode = parameterRepository.findByKey("APP_CODE")
                    .orElseThrow(() -> new RuntimeException("App code not found"));

            SystemParameterModel payDaviviendaUrl = parameterRepository.findByKey("PAY_DAVIVIENDA_URL")
                    .orElseThrow(() -> new RuntimeException("Pay Davivienda URL not found"));

            log.info("url para petición: {}", payDaviviendaUrl.getValue());

            // 2. VALIDACIONES DE SEGURIDAD
            if (bearerTokenRequest == null || !bearerTokenRequest.equals("Bearer " + myBearerToken.getValue())) {
                log.warn("[Handoff-Business] Intento de acceso no autorizado. Token inválido.");
                throw new BusinessException("No autorizado: Bearer Token inválido");
            }

            if (myAppCode.getValue() == null || !myAppCode.getValue().equals(appCode)) {
                log.warn("[Handoff-Business] AppCode no coincide. Recibido: {}, Esperado: {}", appCode, myAppCode);
                throw new BusinessException("Código de aplicación incorrecto");
            }

            // 3. PREPARACIÓN DE PETICIÓN EXTERNA
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Authorization", "Bearer " + payBearerToken.getValue());
            headers.set("Accept", "application/json");

            String jsonBody = "{\"otc\":\"" + otc + "\"}";
            log.info("[DEBUG] JSON Body exacto: {}", jsonBody);
            log.info("[DEBUG] Content-Length: {} bytes",
                    jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            log.info("[Handoff-External] Consultando validación en Davivienda...");

            // disableSslVerification();

            // 4. LLAMADA AL SERVICIO EXTERNO
            ResponseEntity<OtcDTOResponse> response = restTemplate.exchange(
                    payDaviviendaUrl.getValue(),
                    HttpMethod.POST,
                    requestEntity,
                    OtcDTOResponse.class);

            OtcDTOResponse otcResponse = response.getBody();

            // 5. VALIDACIÓN DE RESPUESTA DE NEGOCIO (Proveedor Externo)
            if (otcResponse == null || !Boolean.TRUE.equals(otcResponse.getSuccess())) {
                log.warn("[Handoff-Business] El proveedor rechazó la solicitud o envió respuesta vacía.");
                throw new BusinessException("El OTC no pudo ser procesado por el proveedor");
            }

            String status = otcResponse.getData().getStatus();
            if (!"validated".equals(status)) {
                log.warn("[Handoff-Business] OTC no validado. Status devuelto: {}", status);
                throw new BusinessException("OTC no tiene estado 'validated'");
            }

            // 6. PROCESAMIENTO EXITOSO Y PERSISTENCIA
            String email = otcResponse.getData().getUser();
            String sessionToken = UUID.randomUUID().toString();

            PendingSessionModel pendingSession = new PendingSessionModel();
            pendingSession.setSessionToken(sessionToken);
            pendingSession.setUserEmail(email);
            pendingSession.setCreatedAt(LocalDateTime.now());
            pendingSession.setExpiresAt(LocalDateTime.now().plusMinutes(5));

            pendingSessionRepository.save(pendingSession);
            log.info("[Handoff-Success] Sesión creada para el E-mail: {}. Token generado: {}", email, sessionToken);

            // 7. CONSTRUCCIÓN DE RESPUESTA
            HandoffDTOResponse handoffResponse = new HandoffDTOResponse();
            handoffResponse.setSuccess(true);
            DataSession session = new DataSession();
            session.setStatus("ok");
            session.setToken(sessionToken);
            session.setTimestamp(OffsetDateTime.now());
            handoffResponse.setData(session);

            return handoffResponse;

        } catch (BusinessException e) {
            // AQUÍ CAEN: Tokens inválidos, AppCode erróneo, OTC no validado por Davivienda.
            log.warn("[Handoff-Logic-Error] Error de regla de negocio: {}", e.getMessage());
            throw e;

        } catch (HttpStatusCodeException e) {
            // AQUÍ CAEN: Errores HTTP (404, 500, 401 del servidor de Davivienda).
            log.error("[Handoff-External-Error] Error de comunicación (HTTP {}). Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Error en comunicación con proveedor externo");

        } catch (Exception e) {
            // AQUÍ CAEN: NullPointer, errores de base de datos, errores inesperados.
            log.error("[Handoff-Critical-Error] Error no controlado en el proceso", e);
            throw new RuntimeException("Error interno del sistema: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String activateSession(String sessionToken) {

        // 1. Buscar el token en la BD
        PendingSessionModel session = pendingSessionRepository.findById(sessionToken)
                .orElseThrow(() -> new RuntimeException("invalid_session"));

        // 2. Borrarlo inmediatamente para que sea de un solo uso
        pendingSessionRepository.delete(session);

        // 3. Validar si ya había expirado (aunque lo encontremos, verificamos la hora)
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("session_expired");
        }

        UserModel user = userRepository.findByEmail(IdentifierNormalizer.withoutSeparators(session.getUserEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no registrado en el sistema local."));

        if (user.getStatus() != GeneralStatusEnum.ACTIVE) {
            throw new UnauthorizedAccessException("El usuario se encuentra inactivo.");
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedAccessException(
                    "La entidad a la que pertenece el usuario se encuentra inactiva. Contacte al administrador.");
        }

        return jwtService.generateToken(user);
    }
}
