package com.davivienda.factoraje.dto.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record PayerSummaryDTOResponse(

        UUID id,
        String code,
        String name,
        String nit,
        GeneralStatusEnum status,
        UUID bankAccountId,
        String accountNumber,
        UUID creditFacilityId,
        String creditFacilityNumber,
        BigDecimal facilityLimitAmount,
        BigDecimal amountInUse,
        BigDecimal availableAmount,
        BigDecimal warningThresholdPercentage,
        UUID productPricingTermId,
        BigDecimal interestRate,
        BigDecimal commissionRate,
        CalculationBaseEnum calculationBase

) {

    public static PayerSummaryDTOResponse from(
            EntityModel payer,
            BankAccountModel account,
            CreditFacilityModel facility,
            ProductPricingTermModel pricing) {

        return new PayerSummaryDTOResponse(
                payer.getId(),
                payer.getCode(),
                payer.getName(),
                payer.getNit(),
                payer.getStatus(),
                account != null ? account.getId() : null,
                account != null ? account.getAccountNumber() : null,
                facility != null ? facility.getId() : null,
                facility != null ? facility.getCreditFacilityNumber() : null,
                facility != null ? facility.getFacilityLimitAmount() : null,
                facility != null ? facility.getAmountInUse() : null,
                facility != null ? facility.getAvailableAmount() : null,
                facility != null ? facility.getWarningThresholdPercentage() : null,
                pricing != null ? pricing.getId() : null,
                pricing != null ? pricing.getInterestRate() : null,
                pricing != null ? pricing.getCommissionRate() : null,
                pricing != null ? pricing.getCalculationBase() : null);
    }

}
