package com.davivienda.factoraje.dto.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * El proveedor se registra activo y con su cuenta bancaria principal. El NIU solo es
 * obligatorio aquí: los proveedores creados desde la carga de documentos no lo traen.
 */
public record SupplierRegistrationDTORequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres.")
        String name,

        @NotBlank(message = "El NIT es obligatorio.")
        @Size(max = 25, message = "El NIT no puede superar 25 caracteres.")
        String nit,

        @NotBlank(message = "El NIU es obligatorio.")
        @Size(max = 25, message = "El NIU no puede superar 25 dígitos.")
        @Pattern(regexp = "[\\d\\s-]+", message = "El NIU solo puede contener números.")
        String niu,

        @NotBlank(message = "La cuenta bancaria es obligatoria.")
        @Size(max = 50, message = "La cuenta bancaria no puede superar 50 caracteres.")
        @Pattern(regexp = "[\\d\\s-]+", message = "La cuenta bancaria solo puede contener números.")
        String accountNumber

) {
}
