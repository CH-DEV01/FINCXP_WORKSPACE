package com.davivienda.factoraje.dto.disbursement_policy;

import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.enums.DisbursementPolicyTypeEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record DisbursementPolicyDTOResponse(

        UUID id,
        String code,
        String name,
        String description,
        DisbursementPolicyTypeEnum type,
        String weekdays,
        Integer offsetDays,
        GeneralStatusEnum status

) {

    public static DisbursementPolicyDTOResponse fromEntity(DisbursementPolicyCat model) {

        return new DisbursementPolicyDTOResponse(
                model.getId(),
                model.getCode(),
                model.getName(),
                model.getDescription(),
                model.getType(),
                model.getWeekdays(),
                model.getOffsetDays(),
                model.getStatus());
    }

}
