package com.davivienda.factoraje.dto.dispersion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param dueDates vencimientos seleccionados; se genera un lote por cada uno.
 */
public record GenerateDispersionBatchesRequestDTO(
    @NotNull(message = "El ID del pagador es obligatorio")
    UUID payerId,

    @NotEmpty(message = "Seleccione al menos un vencimiento")
    @Size(max = GenerateDispersionBatchesRequestDTO.MAX_DUE_DATES,
            message = "No se pueden generar más de " + GenerateDispersionBatchesRequestDTO.MAX_DUE_DATES
                    + " lotes a la vez")
    List<@NotNull(message = "Los vencimientos no pueden ser nulos") LocalDate> dueDates
) {
    public static final int MAX_DUE_DATES = 100;
}
