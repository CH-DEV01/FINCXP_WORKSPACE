package com.davivienda.factoraje.dto.disbursement_batch;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record ConfirmDisbursementRequestDTO(
    
    @NotEmpty(message = "Debe enviar al menos un ID de documento")
    List<@NotNull(message = "El ID del documento no puede ser nulo") UUID> documentIds

) {}
