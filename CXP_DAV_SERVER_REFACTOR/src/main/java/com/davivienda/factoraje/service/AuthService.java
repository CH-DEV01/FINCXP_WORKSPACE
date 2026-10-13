package com.davivienda.factoraje.service;

import com.davivienda.factoraje.dto.auth.SessionDTO;
import com.davivienda.factoraje.dto.auth.UserProfileResponseDTO;

public interface AuthService {

    /** Emite el JWT del usuario identificado por su DUI si está activo y habilitado. */
    String authenticateViaSSO(String dui);

    /** Perfil del usuario autenticado, con la vigencia del token de su sesión. */
    UserProfileResponseDTO getCurrentUserProfile(String token);

    /** Emite un token nuevo para la sesión del usuario autenticado. */
    String refreshSession(String token);

    SessionDTO sessionOf(String token);

    /** Invalida todos los tokens emitidos hasta ahora para el usuario autenticado, si lo hay. */
    void logout();
}
