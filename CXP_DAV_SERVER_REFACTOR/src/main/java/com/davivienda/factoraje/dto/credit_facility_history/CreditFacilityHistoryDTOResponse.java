package com.davivienda.factoraje.dto.credit_facility_history;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.CreditFacilityHistoryModel;
import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;

public record CreditFacilityHistoryDTOResponse(

        UUID id,
        UUID creditFacilityId,
        BigDecimal amount,
        Instant date,
        UUID operatorId,
        String operator,
        RepaymentTypeEnum type,
        String reference) {

    public static CreditFacilityHistoryDTOResponse fromEntity(CreditFacilityHistoryModel model) {

        String firstName = model.getExecutedBy().getFirstName() != null ? model.getExecutedBy().getFirstName() : "";
        String lastName = model.getExecutedBy().getLastName() != null ? model.getExecutedBy().getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();

        return new CreditFacilityHistoryDTOResponse(
                model.getId(),
                model.getCreditFacility().getId(),
                model.getAmount(),
                model.getCreatedAt(),
                model.getExecutedBy().getId(),
                fullName,
                model.getRepaymentType(),
                model.getReferenceNumber());
    }

}
