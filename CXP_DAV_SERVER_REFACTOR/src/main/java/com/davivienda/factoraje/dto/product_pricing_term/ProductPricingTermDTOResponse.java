package com.davivienda.factoraje.dto.product_pricing_term;

import java.math.BigDecimal;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record ProductPricingTermDTOResponse(

        UUID id,
        BigDecimal interestRate,
        BigDecimal commissionRate,
        CalculationBaseEnum calculationBase,
        GeneralStatusEnum status,
        UUID creditFacilityId

) {

    public static ProductPricingTermDTOResponse fromEntity(ProductPricingTermModel model) {
        return new ProductPricingTermDTOResponse(
            model.getId(),
            model.getInterestRate(),
            model.getCommissionRate(),
            model.getCalculationBase(),
            model.getStatus(),
            model.getCreditFacility().getId()
        );
    }

}
