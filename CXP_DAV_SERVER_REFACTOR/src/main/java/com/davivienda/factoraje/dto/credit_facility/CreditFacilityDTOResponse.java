package com.davivienda.factoraje.dto.credit_facility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record CreditFacilityDTOResponse(

        UUID id,
        GeneralStatusEnum status,
        BigDecimal facilityLimitAmount,
        BigDecimal amountInUse,
        BigDecimal availableAmount,
        UUID payerId,
        BigDecimal utilizationPercentage,
        BigDecimal warningThresholdPercentage,
        String thresholdStatus,
        BigDecimal interestRate,
        BigDecimal commissionRate,
        CalculationBaseEnum calculationBase

) {

    public static final String THRESHOLD_NORMAL = "NORMAL";
    public static final String THRESHOLD_CRITICAL = "CRITICAL";

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /**
     * warning_threshold_percentage se guarda como fracción (0.80 = 80%) y se
     * expone en porcentaje; si la línea no lo tiene se usa
     * {@code defaultThreshold} (parámetro DEFAULT_CREDIT_THRESHOLD).
     */
    public static CreditFacilityDTOResponse fromEntity(CreditFacilityModel model, ProductPricingTermModel pricing,
            BigDecimal defaultThreshold) {

        BigDecimal utilization = utilizationPercentage(model.getFacilityLimitAmount(), model.getAmountInUse());
        BigDecimal thresholdFraction = model.getWarningThresholdPercentage() != null
                ? model.getWarningThresholdPercentage()
                : defaultThreshold;
        BigDecimal threshold = thresholdFraction.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP);

        return new CreditFacilityDTOResponse(
                model.getId(),
                model.getStatus(),
                model.getFacilityLimitAmount(),
                model.getAmountInUse(),
                model.getAvailableAmount(),
                model.getPayer().getId(),
                utilization,
                threshold,
                utilization.compareTo(threshold) >= 0 ? THRESHOLD_CRITICAL : THRESHOLD_NORMAL,
                pricing != null ? pricing.getInterestRate() : null,
                pricing != null ? pricing.getCommissionRate() : null,
                pricing != null ? pricing.getCalculationBase() : null
        );
    }

    private static BigDecimal utilizationPercentage(BigDecimal limit, BigDecimal inUse) {
        if (limit == null || inUse == null || limit.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return inUse.multiply(ONE_HUNDRED).divide(limit, 2, RoundingMode.HALF_UP);
    }

}
