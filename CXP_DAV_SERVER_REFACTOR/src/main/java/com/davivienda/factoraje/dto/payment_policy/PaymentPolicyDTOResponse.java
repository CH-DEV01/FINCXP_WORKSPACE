package com.davivienda.factoraje.dto.payment_policy;

import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record PaymentPolicyDTOResponse(

        UUID id,
        GeneralStatusEnum status,
        String code,
        String description

) {

    public static PaymentPolicyDTOResponse fromEntity(PaymentPolicyCat model) {
        return new PaymentPolicyDTOResponse(
            model.getId(),
            model.getStatus(),
            model.getCode(),
            model.getDescription()
        );
    }

}
