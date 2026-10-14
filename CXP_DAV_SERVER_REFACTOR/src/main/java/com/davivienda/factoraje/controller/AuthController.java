package com.davivienda.factoraje.controller;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.auth.UserProfileResponseDTO;
import com.davivienda.factoraje.infrastructure.exception.SessionExpiredException;
import com.davivienda.factoraje.infrastructure.security.BearerTokens;
import com.davivienda.factoraje.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record SsoLoginRequest(String dui) {}

    @PostMapping("/sso-login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody SsoLoginRequest request) {
        String jwt = authService.authenticateViaSSO(request.dui());
        return ResponseEntity.ok(ApiResponse.success(jwt, "Sesión establecida correctamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getMe() {
        UserProfileResponseDTO profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile, "Perfil del usuario obtenido correctamente"));
    }

    /** Devuelve un JWT nuevo para la misma sesión; el cliente reemplaza el anterior. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refresh(HttpServletRequest httpRequest) {
        String token = BearerTokens.read(httpRequest)
                .orElseThrow(() -> new SessionExpiredException("La sesión no es válida o expiró."));
        return ResponseEntity.ok(ApiResponse.success(authService.refreshSession(token), "Sesión renovada correctamente"));
    }

    // Público: una sesión ya vencida también puede cerrarse sin error.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success(null, "Sesión cerrada correctamente"));
    }

}
