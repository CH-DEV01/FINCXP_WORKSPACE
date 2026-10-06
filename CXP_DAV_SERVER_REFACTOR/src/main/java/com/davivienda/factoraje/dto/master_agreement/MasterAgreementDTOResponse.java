package com.davivienda.factoraje.dto.master_agreement;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.enums.AgreementTypeEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record MasterAgreementDTOResponse(
        UUID id,
        GeneralStatusEnum status,
        AgreementTypeEnum agreementType,
        UUID payerId,
        String PayerName,
        String payerNit,
        UUID supplierId,
        String supplierName,
        UUID paymentPolicyId,
        Integer paymentPolicyDays,
        UUID disbursementPolicyId,
        String disbursementPolicyName

) {

    public static MasterAgreementDTOResponse fromEntity(MasterAgreementModel model) {

        return new MasterAgreementDTOResponse(
                model.getId(),
                model.getStatus(),
                model.getAgreementType(),
                model.getPayer().getId(),
                model.getPayer().getName(),
                model.getPayer().getNit(),
                model.getSupplier().getId(),
                model.getSupplier().getName(),
                model.getPaymentPolicy().getId(),
                model.getPaymentPolicy().getDaysCount(),
                model.getDisbursementPolicy().getId(),
                model.getDisbursementPolicy().getName()
            );
    }

}
