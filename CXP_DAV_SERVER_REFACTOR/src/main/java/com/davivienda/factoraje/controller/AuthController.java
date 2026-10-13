package com.davivienda.factoraje.controller;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.auth.SessionDTO;
import com.davivienda.factoraje.dto.auth.UserProfileResponseDTO;
import com.davivienda.factoraje.infrastructure.exception.SessionExpiredException;
import com.davivienda.factoraje.infrastructure.security.SessionCookies;
import com.davivienda.factoraje.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionCookies sessionCookies;

    public record SsoLoginRequest(String dui) {}

    @PostMapping("/sso-login")
    public ResponseEntity<ApiResponse<Void>> login(@RequestBody SsoLoginRequest request, HttpServletRequest httpRequest) {
        String jwt = authService.authenticateViaSSO(request.dui());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.create(jwt, httpRequest).toString())
                .body(ApiResponse.success(null, "Sesión establecida correctamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getMe(HttpServletRequest httpRequest) {
        UserProfileResponseDTO profile = authService.getCurrentUserProfile(token(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(profile, "Perfil del usuario obtenido correctamente"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<SessionDTO>> refresh(HttpServletRequest httpRequest) {
        String jwt = authService.refreshSession(token(httpRequest));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.create(jwt, httpRequest).toString())
                .body(ApiResponse.success(authService.sessionOf(jwt), "Sesión renovada correctamente"));
    }

    // Público: también debe borrar la cookie de una sesión vencida.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        authService.logout();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookies.clear(httpRequest).toString())
                .body(ApiResponse.success(null, "Sesión cerrada correctamente"));
    }

    private String token(HttpServletRequest httpRequest) {
        return sessionCookies.read(httpRequest)
                .orElseThrow(() -> new SessionExpiredException("La sesión no es válida o expiró."));
    }

}
