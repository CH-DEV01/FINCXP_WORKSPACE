package com.davivienda.factoraje.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void roundUsesTwoDecimalsHalfUp() {
        assertThat(Money.round(new BigDecimal("10.005"))).isEqualTo(new BigDecimal("10.01"));
        assertThat(Money.round(new BigDecimal("10.004"))).isEqualTo(new BigDecimal("10.00"));
        assertThat(Money.round(new BigDecimal("-10.005"))).isEqualTo(new BigDecimal("-10.01"));
        assertThat(Money.round(new BigDecimal("7"))).isEqualTo(new BigDecimal("7.00"));
        assertThat(Money.round(null)).isNull();
    }

    @Test
    void roundOrZeroTreatsNullAsZero() {
        assertThat(Money.roundOrZero(null)).isEqualTo(new BigDecimal("0.00"));
        assertThat(Money.roundOrZero(new BigDecimal("1.235"))).isEqualTo(new BigDecimal("1.24"));
    }

    @Test
    void formatUsesDollarSignAndThousandsSeparator() {
        assertThat(Money.format(new BigDecimal("1234567.891"))).isEqualTo("$1,234,567.89");
        assertThat(Money.format(new BigDecimal("0.005"))).isEqualTo("$0.01");
        assertThat(Money.format(null)).isEqualTo("$0.00");
    }

    @Test
    void plainHasTwoDecimalsWithoutSymbols() {
        assertThat(Money.plain(new BigDecimal("1234.5"))).isEqualTo("1234.50");
        assertThat(Money.plain(null)).isEqualTo("0.00");
    }

    @Test
    void detectsMoreThanTwoDecimals() {
        assertThat(Money.hasMoreThanTwoDecimals(new BigDecimal("1.001"))).isTrue();
        assertThat(Money.hasMoreThanTwoDecimals(new BigDecimal("1.10"))).isFalse();
        assertThat(Money.hasMoreThanTwoDecimals(new BigDecimal("1.100"))).isFalse();
        assertThat(Money.hasMoreThanTwoDecimals(new BigDecimal("100"))).isFalse();
        assertThat(Money.hasMoreThanTwoDecimals(null)).isFalse();
    }
}
