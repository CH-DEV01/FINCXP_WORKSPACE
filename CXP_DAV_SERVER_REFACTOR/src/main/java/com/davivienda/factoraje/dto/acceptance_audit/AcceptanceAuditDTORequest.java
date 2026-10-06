package com.davivienda.factoraje.dto.acceptance_audit;

import java.util.UUID;

public record AcceptanceAuditDTORequest(

    String userAgent,
    UUID userId,
    UUID versionId

) {
    
}
