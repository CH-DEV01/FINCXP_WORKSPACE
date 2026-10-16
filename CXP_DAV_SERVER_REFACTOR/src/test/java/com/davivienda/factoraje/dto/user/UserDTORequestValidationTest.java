package com.davivienda.factoraje.dto.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class UserDTORequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void firstNameLongerThan255IsRejected() {
        UserDTORequest request = new UserDTORequest(
                "012345678",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "A".repeat(256),
                "Pérez",
                "usuario@banco.com");

        Set<ConstraintViolation<UserDTORequest>> violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> assertThat(violation.getMessage())
                        .contains("255 caracteres"));
    }

    @Test
    void firstNameAt255IsAcceptedWhenOtherFieldsAreValid() {
        UserDTORequest request = new UserDTORequest(
                "012345678",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "A".repeat(255),
                "Pérez",
                "usuario@banco.com");

        assertThat(validator.validate(request)).isEmpty();
    }
}
