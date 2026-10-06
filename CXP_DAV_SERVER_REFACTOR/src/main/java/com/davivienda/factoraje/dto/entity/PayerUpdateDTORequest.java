package com.davivienda.factoraje.dto.entity;

import java.math.BigDecimal;

import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solo expone los atributos editables de un pagador. Las tasas y el umbral se
 * reciben como fracción decimal (0.155 = 15.5%, 0.80 = 80%).
 * La cuenta bancaria solo se puede modificar si el pagador ya tiene una cuenta principal;
 * en ese caso es obligatoria.
 */
public record PayerUpdateDTORequest(

        @Size(max = 50, message = "La cuenta bancaria no puede superar 50 caracteres.")
        @Pattern(regexp = "[\\d\\s-]*", message = "La cuenta bancaria solo puede contener números.")
        String accountNumber,

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

        @NotNull(message = "El estado es obligatorio.")
        GeneralStatusEnum status,

        @NotNull(message = "El umbral es obligatorio.")
        @DecimalMin(value = "0.01", message = "El umbral debe ser mayor a 0%.")
        @DecimalMax(value = "1.0", message = "El umbral no puede superar el 100%.")
        BigDecimal warningThresholdPercentage

) {
}
