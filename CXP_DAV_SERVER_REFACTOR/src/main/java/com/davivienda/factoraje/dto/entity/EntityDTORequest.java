package com.davivienda.factoraje.dto.entity;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EntityDTORequest(

    @NotBlank(message = "El NIT es obligatorio.")
    @Size(max = 25, message = "El NIT no puede superar 25 caracteres.")
    String nit,

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres.")
    String name,

    @NotNull(message = "El tipo de entidad es obligatorio.")
    UUID entityTypeId

){}
