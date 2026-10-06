package com.davivienda.factoraje.dto.product_pricing_term;

import java.math.BigDecimal;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ProductPricingTermDTORequest(

    @NotNull(message = "La tasa de interés es obligatoria.")
    @DecimalMin(value = "0.0", message = "La tasa de interés no puede ser negativa.")
    @DecimalMax(value = "1.0", message = "La tasa de interés no puede superar el 100%.")
    BigDecimal interestRate,

    @NotNull(message = "La tasa de comisión es obligatoria.")
    @DecimalMin(value = "0.0", message = "La tasa de comisión no puede ser negativa.")
    @DecimalMax(value = "1.0", message = "La tasa de comisión no puede superar el 100%.")
    BigDecimal commissionRate,

    @NotNull(message = "La base de cálculo es obligatoria.")
    CalculationBaseEnum calculationBase,

    @NotNull(message = "El ID del cupo de crédito es obligatorio.")
    UUID creditFacilityId

){}
