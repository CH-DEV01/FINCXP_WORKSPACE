package com.davivienda.factoraje.dto.user;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserDTORequest(

    @NotBlank(message = "El DUI es obligatorio.")
    String dui,

    @NotNull(message = "La entidad es obligatoria.")
    UUID entityId,

    @NotNull(message = "El rol es obligatorio.")
    UUID roleId,

    @NotBlank(message = "El nombre es obligatorio.")
    @Pattern(regexp = PERSON_NAME_PATTERN, message = "El nombre solo puede contener letras.")
    String firstName,

    @NotBlank(message = "El apellido es obligatorio.")
    @Pattern(regexp = PERSON_NAME_PATTERN, message = "El apellido solo puede contener letras.")
    String lastName,

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no tiene un formato válido.")
    String email

) {

    /** Palabras de letras separadas por espacios; admite apóstrofos y guiones internos (O'Neil, Pérez-Gómez). */
    public static final String PERSON_NAME_PATTERN = "^\\p{L}+(?:['’-]\\p{L}+)*(?: \\p{L}+(?:['’-]\\p{L}+)*)*$";
}
