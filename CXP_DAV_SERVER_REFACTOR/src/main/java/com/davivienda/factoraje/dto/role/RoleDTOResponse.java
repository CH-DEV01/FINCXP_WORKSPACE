package com.davivienda.factoraje.dto.role;

import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.RoleCat;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record RoleDTOResponse(

        UUID id,
        String name,
        String description,
        GeneralStatusEnum status) {

    public static RoleDTOResponse fromEntity(RoleCat model) {
        return new RoleDTOResponse(
                model.getId(),
                model.getName(),
                model.getDescription(),
                model.getStatus()
        );
    }

}
