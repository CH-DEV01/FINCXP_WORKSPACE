package com.davivienda.factoraje.dto.credit_facility;

import java.math.BigDecimal;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param amount              se ignora: en PARTIAL se abona el nominal del lote; en FULL, el
 *                            consumo actual del cupo.
 * @param disbursementBatchId lote de desembolso que liquida el pago parcial (obligatorio en PARTIAL).
 */
public record RestoreCreditFacilityDTORequest(

        @NotNull(message = "El ID del cupo de crédito es obligatorio")
        UUID creditFacilityId,

        @DecimalMin(value = "0.01", message = "El monto a abonar debe ser mayor a cero")
        @Digits(integer = 15, fraction = 2, message = "El monto a abonar admite máximo 15 enteros y 2 decimales.")
        BigDecimal amount,

        @NotBlank(message = "La referencia es obligatoria")
        @Size(max = 255, message = "La referencia no puede superar 255 caracteres.")
        String reference,

        @NotNull(message = "El tipo de abono es obligatorio")
        RepaymentTypeEnum type, // "PARTIAL" o "FULL"

        UUID disbursementBatchId

) {

}
