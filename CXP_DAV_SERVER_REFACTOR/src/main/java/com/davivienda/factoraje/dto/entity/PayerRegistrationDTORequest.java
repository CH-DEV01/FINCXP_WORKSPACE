package com.davivienda.factoraje.dto.entity;

import java.math.BigDecimal;

import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Las tasas y el umbral se reciben como fracción decimal (0.155 = 15.5%, 0.80 = 80%).
 * No recibe estado: todo pagador, su cupo y su tarifario se registran activos.
 */
public record PayerRegistrationDTORequest(

        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 255, message = "El nombre no puede superar 255 caracteres.")
        String name,

        @NotBlank(message = "El NIT es obligatorio.")
        @Size(max = 25, message = "El NIT no puede superar 25 caracteres.")
        String nit,

        @NotBlank(message = "La cuenta bancaria es obligatoria.")
        @Size(max = 50, message = "La cuenta bancaria no puede superar 50 caracteres.")
        @Pattern(regexp = "[\\d\\s-]+", message = "La cuenta bancaria solo puede contener números.")
        String accountNumber,

        @NotBlank(message = "El número de cupo es obligatorio.")
        @Size(max = 255, message = "El número de cupo no puede superar 255 caracteres.")
        String creditFacilityNumber,

        @NotNull(message = "El monto aprobado del cupo es obligatorio.")
        @DecimalMin(value = "0.01", message = "El monto aprobado del cupo debe ser mayor a cero.")
        BigDecimal facilityLimitAmount,

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

        @NotNull(message = "El umbral es obligatorio.")
        @DecimalMin(value = "0.01", message = "El umbral debe ser mayor a 0%.")
        @DecimalMax(value = "1.0", message = "El umbral no puede superar el 100%.")
        BigDecimal warningThresholdPercentage,

        @DecimalMin(value = "0.0", message = "El monto consumido no puede ser negativo.")
        BigDecimal initialAmountInUse,

        @Size(max = 255, message = "La referencia del consumo no puede superar 255 caracteres.")
        String initialConsumptionReference

) {
}
