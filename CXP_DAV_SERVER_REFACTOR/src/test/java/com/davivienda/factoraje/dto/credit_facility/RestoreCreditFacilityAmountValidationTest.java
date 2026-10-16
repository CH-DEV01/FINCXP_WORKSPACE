package com.davivienda.factoraje.dto.credit_facility;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class RestoreCreditFacilityAmountValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void amountWith16IntegerDigitsIsRejected() {
        assertThat(validator.validate(request(new BigDecimal("1000000000000000.00"))))
                .anySatisfy(violation -> assertThat(violation.getMessage())
                        .contains("15 enteros")
                        .contains("2 decimales"));
    }

    @Test
    void amountWithinDigitsIsAccepted() {
        assertThat(validator.validate(request(new BigDecimal("1500.25")))).isEmpty();
    }

    private static RestoreCreditFacilityDTORequest request(BigDecimal amount) {
        return new RestoreCreditFacilityDTORequest(
                UUID.randomUUID(),
                amount,
                "DEP-001",
                RepaymentTypeEnum.FULL,
                null);
    }
}
