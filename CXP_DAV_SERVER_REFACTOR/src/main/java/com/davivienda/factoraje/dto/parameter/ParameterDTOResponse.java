package com.davivienda.factoraje.dto.parameter;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.SystemParameterModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;

public record ParameterDTOResponse(

        UUID id,
        String key,
        String value,
        boolean sensitive

) {

    public static final String MASK = "********";

    public static ParameterDTOResponse fromEntity(SystemParameterModel model) {
        boolean sensitive = SystemParameterKey.isSensitive(model.getKey());
        // CONFIGURAR se muestra para que se note que el secreto falta en el ambiente.
        String value = sensitive && !SystemParameterKey.UNCONFIGURED.equals(model.getValue())
                ? MASK
                : model.getValue();
        return new ParameterDTOResponse(model.getId(), model.getKey(), value, sensitive);
    }

}
