package com.davivienda.factoraje.dto.entity;

import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solo expone los atributos editables de un proveedor.
 */
public record SupplierUpdateDTORequest(

        @NotBlank(message = "La cuenta bancaria es obligatoria.")
        @Size(max = 50, message = "La cuenta bancaria no puede superar 50 caracteres.")
        @Pattern(regexp = "[\\d\\s-]+", message = "La cuenta bancaria solo puede contener números.")
        String accountNumber,

        @NotNull(message = "El estado es obligatorio.")
        GeneralStatusEnum status

) {
}
