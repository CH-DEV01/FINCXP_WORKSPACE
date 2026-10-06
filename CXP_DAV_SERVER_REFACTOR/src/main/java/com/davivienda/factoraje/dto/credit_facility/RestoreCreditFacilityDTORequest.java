package com.davivienda.factoraje.dto.credit_facility;

import java.math.BigDecimal;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RestoreCreditFacilityDTORequest(

        @NotNull(message = "El ID del cupo de crédito es obligatorio")
        UUID creditFacilityId,

        @NotNull(message = "El monto es obligatorio") 
        @DecimalMin(value = "0.01", message = "El monto a abonar debe ser mayor a cero") BigDecimal amount,

        @NotBlank(message = "La referencia es obligatoria") 
        String reference,

        @NotNull(message = "El tipo de abono es obligatorio") 
        RepaymentTypeEnum type // "PARTIAL" o "FULL"

) {

}
