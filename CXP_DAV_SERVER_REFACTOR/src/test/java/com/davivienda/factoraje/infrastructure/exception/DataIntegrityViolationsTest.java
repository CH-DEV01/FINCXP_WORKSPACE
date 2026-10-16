package com.davivienda.factoraje.infrastructure.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class DataIntegrityViolationsTest {

    @Test
    void stringTruncationIsValueOutOfRange() {
        DataIntegrityViolationException error = wrap("22001");

        assertThat(DataIntegrityViolations.isValueOutOfRange(error)).isTrue();
        assertThat(DataIntegrityViolations.clientMessage(error))
                .isEqualTo(DataIntegrityViolations.VALUE_OUT_OF_RANGE_MESSAGE);
    }

    @Test
    void numericOverflowIsValueOutOfRange() {
        assertThat(DataIntegrityViolations.isValueOutOfRange(wrap("22003"))).isTrue();
    }

    @Test
    void uniqueViolationKeepsConcurrentMessage() {
        DataIntegrityViolationException error = wrap("23505");

        assertThat(DataIntegrityViolations.isValueOutOfRange(error)).isFalse();
        assertThat(DataIntegrityViolations.clientMessage(error))
                .isEqualTo(DataIntegrityViolations.CONCURRENT_WRITE_MESSAGE);
    }

    private static DataIntegrityViolationException wrap(String sqlState) {
        return new DataIntegrityViolationException(
                "could not execute statement",
                new SQLException("simulated", sqlState));
    }
}
