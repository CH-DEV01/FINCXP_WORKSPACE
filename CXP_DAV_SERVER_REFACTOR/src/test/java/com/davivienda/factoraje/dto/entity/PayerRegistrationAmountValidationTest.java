package com.davivienda.factoraje.dto.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PayerRegistrationAmountValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void facilityLimitWith16IntegerDigitsIsRejected() {
        assertThat(validator.validate(request(new BigDecimal("1000000000000000.00"))))
                .anySatisfy(violation -> assertThat(violation.getMessage())
                        .contains("15 enteros")
                        .contains("2 decimales"));
    }

    @Test
    void facilityLimitWithMoreThanTwoFractionDigitsIsRejected() {
        assertThat(validator.validate(request(new BigDecimal("100.123"))))
                .anySatisfy(violation -> assertThat(violation.getMessage()).contains("2 decimales"));
    }

    @Test
    void facilityLimitWithinDigitsIsAccepted() {
        assertThat(validator.validate(request(new BigDecimal("999999999999999.99")))).isEmpty();
    }

    private static PayerRegistrationDTORequest request(BigDecimal facilityLimit) {
        return new PayerRegistrationDTORequest(
                "Pagador de prueba",
                "06140101901011",
                "123456789",
                "0012345678",
                "1001",
                facilityLimit,
                new BigDecimal("0.10"),
                new BigDecimal("0.05"),
                CalculationBaseEnum.COMERCIAL_360,
                new BigDecimal("0.80"),
                BigDecimal.ZERO,
                null);
    }
}
