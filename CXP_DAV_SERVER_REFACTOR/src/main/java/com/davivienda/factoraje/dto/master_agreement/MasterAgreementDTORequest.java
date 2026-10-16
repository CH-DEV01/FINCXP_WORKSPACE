package com.davivienda.factoraje.dto.master_agreement;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/** Actualización de un convenio: solo las políticas de pago y desembolso son editables. */
public record MasterAgreementDTORequest(

    @NotNull(message = "La política de pago es obligatoria.")
    UUID paymentPolicyId,

    @NotNull(message = "La política de desembolso es obligatoria.")
    UUID disbursementPolicyId

) {
}
