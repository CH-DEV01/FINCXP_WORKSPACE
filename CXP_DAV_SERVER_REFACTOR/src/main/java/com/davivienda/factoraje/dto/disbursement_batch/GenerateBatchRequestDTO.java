package com.davivienda.factoraje.dto.disbursement_batch;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param groups combinación (vencimiento, solicitud, desembolso) a convertir en
 *               lote. Se admite una sola porque la respuesta es un único PDF.
 */
public record GenerateBatchRequestDTO(
    @NotBlank(message = "El nombre del archivo original es obligatorio")
    String originalFileName,

    @NotNull(message = "El ID del pagador (Payer) es obligatorio")
    UUID payerId,

    @NotNull(message = "Debe seleccionar la solicitud a generar")
    @Size(min = 1, max = 1, message = "Seleccione una sola solicitud para generar el lote")
    List<@Valid Group> groups
) {

    public record Group(
        @NotNull(message = "La fecha de vencimiento del grupo es obligatoria")
        LocalDate dueDate,

        @NotNull(message = "La fecha de solicitud del grupo es obligatoria")
        LocalDate requestDate,

        @NotNull(message = "La fecha de desembolso del grupo es obligatoria")
        LocalDate disbursementDate
    ) {}
}
