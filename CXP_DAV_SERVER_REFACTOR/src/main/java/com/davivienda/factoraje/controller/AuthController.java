package com.davivienda.factoraje.controller;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.auth.UserProfileResponseDTO;
import com.davivienda.factoraje.service.AuthService;
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
        return ResponseEntity.ok(ApiResponse.success(jwt,"Sesión establecida correctamente"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponseDTO>> getMe() {
        UserProfileResponseDTO profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile, "Perfil del usuario obtenido correctamente"));
    }

}
