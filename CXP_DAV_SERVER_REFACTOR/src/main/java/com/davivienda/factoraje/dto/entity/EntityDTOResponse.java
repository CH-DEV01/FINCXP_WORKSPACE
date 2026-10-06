package com.davivienda.factoraje.dto.entity;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.EntityModel;

public record EntityDTOResponse(

        UUID id,
        String name,
        String code,
        String nit,
        String status,
        String entityTypeId,
        String entityTypeName

) {

    public static EntityDTOResponse fromEntity(EntityModel model) {
        return new EntityDTOResponse(
                model.getId(),
                model.getName(),
                model.getCode(),
                model.getNit(),
                model.getStatus().name(),
                model.getEntityType().getId().toString(),
                model.getEntityType().getName());
    }   

}
