package com.davivienda.factoraje.dto.funding_request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param expectedDisbursementDate    fecha de desembolso que mostró la simulación aceptada.
 * @param expectedAmountToBeDisbursed total a desembolsar que mostró la simulación aceptada.
 *                                    Si al registrar la solicitud alguno difiere (por ejemplo,
 *                                    se cruzó la hora de corte), se responde 409.
 */
public record SubmitFundingDTORequest(
        @NotNull(message = "El ID del Convenio Marco es obligatorio.")
        UUID masterAgreementId,

        @NotEmpty(message = "Debe incluir al menos un ID de documento para financiar.")
        @Size(max = MAX_DOCUMENTS, message = "No se pueden incluir más de {max} documentos por solicitud.")
        List<@NotNull UUID> documentIds,

        @NotNull(message = "El ID de la versión de los términos y condiciones es obligatorio.")
        UUID termVersionId,

        @NotNull(message = "La fecha de desembolso de la simulación aceptada es obligatoria.")
        LocalDate expectedDisbursementDate,

        @NotNull(message = "El monto a desembolsar de la simulación aceptada es obligatorio.")
        BigDecimal expectedAmountToBeDisbursed
) {
    public static final int MAX_DOCUMENTS = 500;
}
