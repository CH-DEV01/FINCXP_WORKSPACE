package com.davivienda.factoraje.service;

import com.davivienda.factoraje.dto.sso.HandoffDTOResponse;

public interface SsoService {

    public HandoffDTOResponse processHandoff(String appCode, String otc, String bearerTokenRequest);

    public String activateSession(String sessionToken);
    
}
