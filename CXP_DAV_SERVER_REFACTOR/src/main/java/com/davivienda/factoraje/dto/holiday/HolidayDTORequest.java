package com.davivienda.factoraje.dto.holiday;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HolidayDTORequest(

        @NotNull(message = "La fecha del día feriado es obligatoria.")
        LocalDate holidayDate,

        @NotBlank(message = "La descripción del día feriado es obligatoria.")
        @Size(max = 255, message = "La descripción no puede superar 255 caracteres.")
        String description
) {
}
