package com.davivienda.factoraje.dto.term_version;

import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.TermTypeCat;

public record TermTypeDTOResponse(
        UUID id,
        String termName,
        String uniqueCode
) {
    public static TermTypeDTOResponse fromEntity(TermTypeCat model) {
        return new TermTypeDTOResponse(model.getId(), model.getTermName(), model.getUniqueCode());
    }
}
