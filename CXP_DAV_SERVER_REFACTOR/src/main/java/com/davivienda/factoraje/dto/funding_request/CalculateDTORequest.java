package com.davivienda.factoraje.dto.funding_request;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CalculateDTORequest(
        @NotNull(message = "El ID del Convenio Marco es obligatorio.") UUID masterAgreementId,

        @NotEmpty(message = "Debe incluir al menos un ID de documento para calcular.")
        @Size(max = SubmitFundingDTORequest.MAX_DOCUMENTS,
                message = "No se pueden incluir más de {max} documentos por solicitud.")
        List<@NotNull UUID> documentIds) {
}
