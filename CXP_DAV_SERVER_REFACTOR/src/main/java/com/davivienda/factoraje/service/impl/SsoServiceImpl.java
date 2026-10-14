package com.davivienda.factoraje.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

        log.info("[Handoff-Start] Iniciando proceso para AppCode: {}", appCode);

        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.info("[SSO-TRACE][handoff][{}] 1. Entrada: app='{}', otc='{}', Authorization='{}'",
                traceId, appCode, otc, bearerTokenRequest);

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
            log.info("[SSO-TRACE][handoff][{}] 2. Parámetros: BEARER_TOKEN(id={})='{}', PAY_BEARER_TOKEN(id={})='{}', "
                    + "APP_CODE(id={})='{}', PAY_DAVIVIENDA_URL(id={})='{}'",
                    traceId,
                    myBearerToken.getId(), myBearerToken.getValue(),
                    payBearerToken.getId(), payBearerToken.getValue(),
                    myAppCode.getId(), myAppCode.getValue(),
                    payDaviviendaUrl.getId(), payDaviviendaUrl.getValue());

            // 2. VALIDACIONES DE SEGURIDAD
            boolean bearerOk = secretEquals("Bearer " + myBearerToken.getValue(), bearerTokenRequest);
            log.info("[SSO-TRACE][handoff][{}] 3. Bearer: esperado='{}', recibido='{}', coincide={}",
                    traceId, "Bearer " + myBearerToken.getValue(), bearerTokenRequest, bearerOk);
            if (!bearerOk) {
                log.warn("[Handoff-Business] Intento de acceso no autorizado. Token inválido.");
                throw new BusinessException("No autorizado: Bearer Token inválido");
            }

            boolean appCodeOk = myAppCode.getValue() != null && secretEquals(myAppCode.getValue(), appCode);
            log.info("[SSO-TRACE][handoff][{}] 4. AppCode: esperado='{}', recibido='{}', coincide={}",
                    traceId, myAppCode.getValue(), appCode, appCodeOk);
            if (!appCodeOk) {
                log.warn("[Handoff-Business] AppCode no coincide.");
                throw new BusinessException("Código de aplicación incorrecto");
            }

            // 3. PREPARACIÓN DE PETICIÓN EXTERNA
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Authorization", "Bearer " + payBearerToken.getValue());
            headers.set("Accept", "application/json");

            String jsonBody = "{\"otc\":\"" + otc + "\"}";
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            log.info("[Handoff-External] Consultando validación en Davivienda...");
            log.info("[SSO-TRACE][handoff][{}] 5. Petición a Davivienda: POST {} headers={} body={}",
                    traceId, payDaviviendaUrl.getValue(), headers, jsonBody);

            // disableSslVerification();

            // 4. LLAMADA AL SERVICIO EXTERNO
            long started = System.currentTimeMillis();
            ResponseEntity<OtcDTOResponse> response = restTemplate.exchange(
                    payDaviviendaUrl.getValue(),
                    HttpMethod.POST,
                    requestEntity,
                    OtcDTOResponse.class);

            OtcDTOResponse otcResponse = response.getBody();
            log.info("[SSO-TRACE][handoff][{}] 6. Respuesta de Davivienda en {} ms: status={} headers={} body={}",
                    traceId, System.currentTimeMillis() - started, response.getStatusCode(),
                    response.getHeaders(), otcResponse);

            // 5. VALIDACIÓN DE RESPUESTA DE NEGOCIO (Proveedor Externo)
            if (otcResponse == null || !Boolean.TRUE.equals(otcResponse.getSuccess())) {
                log.warn("[Handoff-Business] El proveedor rechazó la solicitud o envió respuesta vacía.");
                throw new BusinessException("El OTC no pudo ser procesado por el proveedor");
            }

            String status = otcResponse.getData().getStatus();
            log.info("[SSO-TRACE][handoff][{}] 7. Estado del OTC: '{}' (se espera 'validated')", traceId, status);
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
            log.info("[Handoff-Success] Sesión creada para el E-mail: {}", email);
            log.info("[SSO-TRACE][handoff][{}] 8. Sesión pendiente guardada: sessionToken={}, user='{}', "
                    + "userEmail='{}', createdAt={}, expiresAt={}",
                    traceId, sessionToken, email, otcResponse.getData().getUserEmail(),
                    pendingSession.getCreatedAt(), pendingSession.getExpiresAt());

            // 7. CONSTRUCCIÓN DE RESPUESTA
            HandoffDTOResponse handoffResponse = new HandoffDTOResponse();
            handoffResponse.setSuccess(true);
            DataSession session = new DataSession();
            session.setStatus("ok");
            session.setToken(sessionToken);
            session.setTimestamp(OffsetDateTime.now());
            handoffResponse.setData(session);

            log.info("[SSO-TRACE][handoff][{}] 9. Respuesta del handoff: {}", traceId, handoffResponse);
            return handoffResponse;

        } catch (BusinessException e) {
            // AQUÍ CAEN: Tokens inválidos, AppCode erróneo, OTC no validado por Davivienda.
            log.warn("[Handoff-Logic-Error] Error de regla de negocio: {}", e.getMessage());
            log.warn("[SSO-TRACE][handoff][{}] FIN con error de negocio: {}", traceId, e.getMessage());
            throw e;

        } catch (HttpStatusCodeException e) {
            // AQUÍ CAEN: Errores HTTP (404, 500, 401 del servidor de Davivienda).
            log.error("[Handoff-External-Error] Error de comunicación (HTTP {}). Response: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            log.error("[SSO-TRACE][handoff][{}] FIN con error HTTP de Davivienda: status={} headers={} body={}",
                    traceId, e.getStatusCode(), e.getResponseHeaders(), e.getResponseBodyAsString());
            throw new RuntimeException("Error en comunicación con proveedor externo");

        } catch (Exception e) {
            // AQUÍ CAEN: NullPointer, errores de base de datos, errores inesperados.
            log.error("[Handoff-Critical-Error] Error no controlado en el proceso", e);
            log.error("[SSO-TRACE][handoff][{}] FIN con error no controlado: {}: {}",
                    traceId, e.getClass().getName(), e.getMessage());
            throw new RuntimeException("Error interno del sistema: " + e.getMessage());
        }
    }

    /** Compara en tiempo constante para no revelar cuántos caracteres coinciden. */
    static boolean secretEquals(String expected, String received) {
        if (expected == null || received == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional
    public String activateSession(String sessionToken) {

        log.info("[SSO-TRACE][activate][{}] 1. Entrada: sessionToken='{}'", sessionToken, sessionToken);

        // 1. Buscar el token en la BD
        PendingSessionModel session = pendingSessionRepository.findById(sessionToken)
                .orElseThrow(() -> {
                    log.warn("[SSO-TRACE][activate][{}] FIN: no existe en sso_pending_sessions "
                            + "(ya se usó o nunca se creó)", sessionToken);
                    return new RuntimeException("invalid_session");
                });
        log.info("[SSO-TRACE][activate][{}] 2. Sesión pendiente encontrada: userEmail='{}', createdAt={}, expiresAt={}",
                sessionToken, session.getUserEmail(), session.getCreatedAt(), session.getExpiresAt());

        // 2. Borrarlo inmediatamente para que sea de un solo uso
        pendingSessionRepository.delete(session);
        log.info("[SSO-TRACE][activate][{}] 3. Sesión pendiente eliminada (un solo uso)", sessionToken);

        // 3. Validar si ya había expirado (aunque lo encontremos, verificamos la hora)
        LocalDateTime now = LocalDateTime.now();
        log.info("[SSO-TRACE][activate][{}] 4. Vigencia: ahora={}, expiresAt={}, vencida={}",
                sessionToken, now, session.getExpiresAt(), session.getExpiresAt().isBefore(now));
        if (session.getExpiresAt().isBefore(now)) {
            throw new RuntimeException("session_expired");
        }

        String normalizedEmail = IdentifierNormalizer.withoutSeparators(session.getUserEmail());
        log.info("[SSO-TRACE][activate][{}] 5. Búsqueda de usuario: userEmail='{}', normalizado='{}'",
                sessionToken, session.getUserEmail(), normalizedEmail);
        UserModel user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> {
                    log.warn("[SSO-TRACE][activate][{}] FIN: no hay usuario con email '{}'", sessionToken, normalizedEmail);
                    return new ResourceNotFoundException("Usuario no registrado en el sistema local.");
                });

        log.info("[SSO-TRACE][activate][{}] 6. Usuario: id={}, email='{}', status={}, enabled={}, rol={}, entidad={} ({})",
                sessionToken, user.getId(), user.getEmail(), user.getStatus(), user.isEnabled(),
                user.getRole() != null ? user.getRole().getName() : null,
                user.getEntity() != null ? user.getEntity().getId() : null,
                user.getEntity() != null ? user.getEntity().getStatus() : null);

        if (user.getStatus() != GeneralStatusEnum.ACTIVE) {
            throw new UnauthorizedAccessException("El usuario se encuentra inactivo.");
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedAccessException(
                    "La entidad a la que pertenece el usuario se encuentra inactiva. Contacte al administrador.");
        }

        String jwt = jwtService.generateToken(user);
        log.info("[SSO-TRACE][activate][{}] 7. JWT emitido para el usuario {}: {}", sessionToken, user.getId(), jwt);
        return jwt;
    }
}
