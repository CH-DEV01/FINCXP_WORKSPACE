package com.davivienda.factoraje.dto.acceptance_audit;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.AcceptanceAuditModel;

public record AcceptanceAuditDTOResponse(

    UUID id,
    String userAgent,
    UUID userId,
    UUID versionId

) {

    public static AcceptanceAuditDTOResponse fromEntity(AcceptanceAuditModel model){
        return new AcceptanceAuditDTOResponse(
            model.getId(),
            model.getUserAgent(),
            model.getUser().getId(),
            model.getTermVersion().getId()
        );
    }

}
