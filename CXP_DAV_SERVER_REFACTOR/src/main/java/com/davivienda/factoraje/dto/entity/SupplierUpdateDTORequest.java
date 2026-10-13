package com.davivienda.factoraje.dto.entity;

import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solo expone los atributos editables de un proveedor. El NIU es obligatorio: los proveedores
 * creados desde la carga no lo traen y lo reciben la primera vez que se editan.
 */
public record SupplierUpdateDTORequest(

        @NotBlank(message = "El NIU es obligatorio.")
        @Size(max = 25, message = "El NIU no puede superar 25 dígitos.")
        @Pattern(regexp = "[\\d\\s-]+", message = "El NIU solo puede contener números.")
        String niu,

        @NotBlank(message = "La cuenta bancaria es obligatoria.")
        @Size(max = 50, message = "La cuenta bancaria no puede superar 50 caracteres.")
        @Pattern(regexp = "[\\d\\s-]+", message = "La cuenta bancaria solo puede contener números.")
        String accountNumber,

        @NotNull(message = "El estado es obligatorio.")
        GeneralStatusEnum status

) {
}
