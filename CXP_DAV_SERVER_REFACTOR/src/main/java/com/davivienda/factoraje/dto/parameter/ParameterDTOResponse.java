package com.davivienda.factoraje.dto.parameter;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.SystemParameterModel;

public record ParameterDTOResponse(

        UUID id,
        String key,
        String value

) {

    public static ParameterDTOResponse fromEntity(SystemParameterModel model) {
        return new ParameterDTOResponse(
                model.getId(),
                model.getKey(),
                model.getValue());
    }

}