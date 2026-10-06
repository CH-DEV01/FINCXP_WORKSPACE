package com.davivienda.factoraje.dto.disbursement_policy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NextDisbursementDateDTOResponse(
        LocalDate nextDisbursementDate,
        String policyName,
        String policyCode,
        UUID masterAgreementId,
        LocalTime cutoffTime
) {
}
