package com.davivienda.factoraje.dto.dispersion;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * @param dueDate vencimiento de los documentos que forman el lote; también es la fecha de dispersión.
 */
public record GenerateDispersionBatchRequestDTO(
    @NotNull(message = "El ID del pagador es obligatorio")
    UUID payerId,

    @NotNull(message = "La fecha de vencimiento del grupo es obligatoria")
    LocalDate dueDate
) {}
