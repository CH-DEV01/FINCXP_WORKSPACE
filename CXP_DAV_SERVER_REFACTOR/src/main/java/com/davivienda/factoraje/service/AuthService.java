package com.davivienda.factoraje.service;

import com.davivienda.factoraje.dto.auth.UserProfileResponseDTO;

public interface AuthService {

    /** Emite el JWT del usuario identificado por su DUI si está activo y habilitado. */
    String authenticateViaSSO(String dui);

    UserProfileResponseDTO getCurrentUserProfile();
}
