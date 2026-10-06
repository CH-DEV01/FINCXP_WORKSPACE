package com.davivienda.factoraje.service;

import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTORequest;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTOResponse;

public interface AcceptanceAuditService {

    /**
     * El {@code userId} de la solicitud debe ser el del usuario autenticado: quien
     * llama lo toma de {@code CurrentUserService}, nunca del cuerpo de la petición.
     */
    AcceptanceAuditDTOResponse createAcceptanceAudit(AcceptanceAuditDTORequest request);
}
