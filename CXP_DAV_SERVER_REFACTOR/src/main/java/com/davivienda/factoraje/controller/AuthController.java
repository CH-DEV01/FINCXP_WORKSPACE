package com.davivienda.factoraje.controller;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.auth.SessionWindowDTO;
import com.davivienda.factoraje.dto.auth.UserProfileResponseDTO;
import com.davivienda.factoraje.infrastructure.exception.SessionExpiredException;
import com.davivienda.factoraje.infrastructure.security.JwtService;
import com.davivienda.factoraje.infrastructure.security.SessionCookies;
import com.davivienda.factoraje.infrastructure.security.SessionTokens;
import com.davivienda.factoraje.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * El JWT viaja en la cookie HttpOnly de sesión y no se devuelve en el cuerpo: el
 * cliente solo recibe la vigencia para programar la renovación.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final SessionCookies sessionCookies;
    private final SessionTokens sessionTokens;

    public record SsoLoginRequest(String dui) {}

    @PostMapping("/sso-login")
    public ResponseEntity<ApiResponse<SessionWindowDTO>> login(@RequestBody SsoLoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String jwt = authService.authenticateViaSSO(request.dui());
        sessionCookies.write(httpRequest, httpResponse, jwt);
        return ResponseEntity.ok(ApiResponse.success(jwtService.window(jwt), "Sesión establecida correctamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getMe(HttpServletRequest httpRequest) {
        UserProfileResponseDTO profile = authService.getCurrentUserProfile(currentToken(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(profile, "Perfil del usuario obtenido correctamente"));
    }

    /** Reemplaza la cookie por un JWT nuevo de la misma sesión. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<SessionWindowDTO>> refresh(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String jwt = authService.refreshSession(currentToken(httpRequest));
        sessionCookies.write(httpRequest, httpResponse, jwt);
        return ResponseEntity.ok(ApiResponse.success(jwtService.window(jwt), "Sesión renovada correctamente"));
    }

    // Público: una sesión ya vencida también puede cerrarse sin error.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        authService.logout();
        sessionCookies.clear(httpRequest, httpResponse);
        return ResponseEntity.ok(ApiResponse.success(null, "Sesión cerrada correctamente"));
    }

    private String currentToken(HttpServletRequest httpRequest) {
        return sessionTokens.read(httpRequest)
                .orElseThrow(() -> new SessionExpiredException("La sesión no es válida o expiró."));
    }
}
