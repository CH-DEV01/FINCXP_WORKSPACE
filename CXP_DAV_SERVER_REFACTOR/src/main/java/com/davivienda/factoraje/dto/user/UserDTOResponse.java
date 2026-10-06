package com.davivienda.factoraje.dto.user;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record UserDTOResponse(

        UUID id,
        String firstName,
        String LastName,
        String dui,
        UUID entityId,
        String entityName,
        UUID roleId,
        String roleName,
        GeneralStatusEnum status,
        String email) {

    public static UserDTOResponse fromEntity(UserModel model) {
        return new UserDTOResponse(
                model.getId(),
                model.getFirstName(),
                model.getLastName(),
                model.getDui(),
                model.getEntity().getId(),
                model.getEntity().getName(),
                model.getRole().getId(),
                model.getRole().getName(),
                model.getStatus(),
                model.getEmail());
    }

}
