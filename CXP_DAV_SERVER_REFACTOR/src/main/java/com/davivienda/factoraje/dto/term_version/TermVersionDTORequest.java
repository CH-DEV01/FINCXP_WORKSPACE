package com.davivienda.factoraje.dto.term_version;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos de un borrador. termTypeCode solo se usa al crear; una versión no cambia de tipo.
 */
public record TermVersionDTORequest(
        String termTypeCode,

        @NotBlank(message = "El número de versión es obligatorio.")
        @Pattern(regexp = "^\\d+(\\.\\d+){0,2}$", message = "El número de versión debe tener el formato 1, 1.1 o 1.1.1.")
        @Size(max = 20, message = "El número de versión no puede superar 20 caracteres.")
        String versionNumber,

        @NotBlank(message = "El título es obligatorio.")
        @Size(max = 255, message = "El título no puede superar 255 caracteres.")
        String title,

        @NotBlank(message = "El contenido es obligatorio.")
        @Size(max = 100000, message = "El contenido no puede superar 100000 caracteres.")
        String content,

        @NotBlank(message = "El texto de aceptación es obligatorio.")
        @Size(max = 500, message = "El texto de aceptación no puede superar 500 caracteres.")
        String acceptanceText,

        @Size(max = 500, message = "El enlace del documento no puede superar 500 caracteres.")
        @Pattern(regexp = "^$|^https://\\S+$", message = "El enlace del documento debe comenzar con https://.")
        String documentUrl
) {
}
