package com.davivienda.factoraje.dto.master_agreement;

import java.util.UUID;

import com.davivienda.factoraje.domain.enums.AgreementTypeEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

import jakarta.validation.constraints.NotNull;

/**
 * Compartido por creación y actualización: tipo, pagador y proveedor solo se
 * exigen al crear y el estado solo al actualizar; eso lo valida el servicio.
 */
public record MasterAgreementDTORequest(

    AgreementTypeEnum agreementType,
    UUID payerId,
    UUID supplierId,

    @NotNull(message = "La política de pago es obligatoria.")
    UUID paymentPolicyId,

    @NotNull(message = "La política de desembolso es obligatoria.")
    UUID disbursementPolicyId,

    GeneralStatusEnum status

){

}
