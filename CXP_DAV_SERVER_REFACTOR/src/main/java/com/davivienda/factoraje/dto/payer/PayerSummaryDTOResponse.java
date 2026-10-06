package com.davivienda.factoraje.dto.payer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

/** Datos del pagador autenticado para su propio módulo; {@code creditLine} es null si no tiene línea. */
public record PayerSummaryDTOResponse(
        UUID id,
        String name,
        String nit,
        String code,
        CreditLine creditLine
) {

    /**
     * @param thresholdPercentage umbral de alerta en porcentaje (80.00 = 80%).
     * @param uploadLimitAmount   uso máximo que admite una carga: límite × umbral.
     * @param availableToUpload   lo que aún puede cargarse sin superar {@code uploadLimitAmount}.
     */
    public record CreditLine(
            GeneralStatusEnum status,
            BigDecimal limitAmount,
            BigDecimal amountInUse,
            BigDecimal availableAmount,
            BigDecimal utilizationPercentage,
            BigDecimal thresholdPercentage,
            BigDecimal uploadLimitAmount,
            BigDecimal availableToUpload
    ) {}

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public static PayerSummaryDTOResponse of(EntityModel payer, CreditFacilityModel facility,
            BigDecimal defaultThreshold) {
        return new PayerSummaryDTOResponse(payer.getId(), payer.getName(), payer.getNit(), payer.getCode(),
                facility != null ? creditLine(facility, defaultThreshold) : null);
    }

    private static CreditLine creditLine(CreditFacilityModel facility, BigDecimal defaultThreshold) {
        BigDecimal limit = facility.getFacilityLimitAmount();
        BigDecimal inUse = facility.getAmountInUse() != null ? facility.getAmountInUse() : BigDecimal.ZERO;
        BigDecimal threshold = facility.getWarningThresholdPercentage() != null
                ? facility.getWarningThresholdPercentage()
                : defaultThreshold;

        return new CreditLine(
                facility.getStatus(),
                limit,
                inUse,
                facility.getAvailableAmount(),
                limit.signum() > 0
                        ? inUse.multiply(ONE_HUNDRED).divide(limit, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO.setScale(2),
                threshold.multiply(ONE_HUNDRED).setScale(2, RoundingMode.HALF_UP),
                facility.uploadLimitAmount(defaultThreshold),
                facility.availableToUpload(defaultThreshold));
    }
}
