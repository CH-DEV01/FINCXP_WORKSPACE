package com.davivienda.factoraje.dto.user;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserDTORequest(

    @NotBlank(message = "El DUI es obligatorio.")
    String dui,

    @NotNull(message = "La entidad es obligatoria.")
    UUID entityId,

    @NotNull(message = "El rol es obligatorio.")
    UUID roleId,

    @NotBlank(message = "El nombre es obligatorio.")
    String firstName,

    String lastName,

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no tiene un formato válido.")
    String email

) {
    
}
