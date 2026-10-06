package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.service.impl.FundingCalculator.CalculationResult;
import com.davivienda.factoraje.service.impl.FundingCalculator.DocumentMathResult;

class FundingCalculatorTest {

    private static final LocalDate DISBURSEMENT_DATE = LocalDate.of(2026, 10, 2);
    private static final FundingCalculator.Rates RATES = new FundingCalculator.Rates(
            new BigDecimal("0.12"), new BigDecimal("0.0025"), 360, new BigDecimal("0.13"));

    private final FundingCalculator calculator = new FundingCalculator();

    @Test
    void discountFactorKeepsAllDecimalsUpToStorageScale() {
        assertThat(calculator.calculateDiscountFactor(new BigDecimal("0.12"), 360, 31))
                .isEqualTo(new BigDecimal("0.989772352358957440"));
        assertThat(calculator.calculateDiscountFactor(new BigDecimal("0.12"), 360, 61))
                .isEqualTo(new BigDecimal("0.980071871937275400"));
    }

    @Test
    void discountFactorIsOneWithoutFinancingDays() {
        assertThat(calculator.calculateDiscountFactor(new BigDecimal("0.12"), 360, 0)).isEqualTo(BigDecimal.ONE);
        assertThat(calculator.calculateDiscountFactor(new BigDecimal("0.12"), 360, -5)).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void discountFactorRejectsZeroBase() {
        assertThatThrownBy(() -> calculator.calculateDiscountFactor(new BigDecimal("0.12"), 0, 30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base");
    }

    @Test
    void financingDaysCountBothEnds() {
        assertThat(calculator.financingDays(DISBURSEMENT_DATE, DISBURSEMENT_DATE)).isEqualTo(1);
        assertThat(calculator.financingDays(DISBURSEMENT_DATE, LocalDate.of(2026, 11, 1))).isEqualTo(31);
    }

    @Test
    void documentAmountsKeepAllDecimalsAndAddUpToNominal() {
        DocumentMathResult result = calculator.calculateDocument(
                document("1000.00", LocalDate.of(2026, 11, 1)), RATES, DISBURSEMENT_DATE);

        assertThat(result.diffDays()).isEqualTo(31);
        assertThat(result.nominalAmount()).isEqualByComparingTo("1000.00");
        assertThat(result.amountToFinance()).isEqualByComparingTo("989.772352358957440000");
        assertThat(result.interest()).isEqualByComparingTo("10.227647641042560000");
        assertThat(result.commission()).isEqualByComparingTo("2.474430880897393600");
        assertThat(result.ivaAmount()).isEqualByComparingTo("0.321676014516661168");
        assertThat(result.commissionWithIva()).isEqualByComparingTo("2.796106895414054768");
        assertThat(result.disburse()).isEqualByComparingTo("986.976245463543385232");
        assertThat(result.discountRate()).isEqualByComparingTo("0.989772352358957440");
        assertThat(result.financingPercentage()).isEqualByComparingTo("0.010227647641042560");

        assertThat(result.interest().add(result.commission()).add(result.ivaAmount()).add(result.disburse()))
                .isEqualByComparingTo(result.nominalAmount());
    }

    @Test
    void commissionWithIvaIsShownFromTheExactSumLikeASpreadsheet() {
        DocumentMathResult result = calculator.calculateDocument(
                document("1000.00", LocalDate.of(2026, 11, 1)), RATES, DISBURSEMENT_DATE);

        assertThat(Money.round(result.commission())).isEqualByComparingTo("2.47");
        assertThat(Money.round(result.ivaAmount())).isEqualByComparingTo("0.32");
        assertThat(Money.round(result.commissionWithIva())).isEqualByComparingTo("2.80");
    }

    @Test
    void ivaIsCalculatedOnTheUnroundedCommission() {
        FundingCalculator.Rates rates = new FundingCalculator.Rates(
                new BigDecimal("0.12"), new BigDecimal("0.005"), 360, new BigDecimal("0.13"));

        DocumentMathResult result = calculator.calculateDocument(
                document("3853.50", DISBURSEMENT_DATE.minusDays(1)), rates, DISBURSEMENT_DATE);

        assertThat(result.commission()).isEqualByComparingTo("19.2675");
        assertThat(Money.round(result.ivaAmount())).isEqualByComparingTo("2.50");
    }

    @Test
    void totalsAreTheExactSumAndRoundOnlyWhenShown() {
        CalculationResult result = calculator.calculate(List.of(
                document("1000.00", LocalDate.of(2026, 11, 1)),
                document("2500.55", LocalDate.of(2026, 12, 1))), RATES, DISBURSEMENT_DATE);

        assertThat(result.details()).hasSize(2);
        assertThat(Money.round(result.details().get(0).disburse())).isEqualByComparingTo("986.98");
        assertThat(Money.round(result.details().get(1).disburse())).isEqualByComparingTo("2443.80");

        assertThat(result.totalAmount()).isEqualByComparingTo("3500.55");
        assertThat(result.totalAmountToFinance()).isEqualByComparingTo("3440.491071731711441470");
        assertThat(result.totalAmountToDisburse()).isEqualByComparingTo("3430.771684454069356647");
        assertThat(Money.round(result.totalAmountToFinance())).isEqualByComparingTo("3440.49");
        assertThat(Money.round(result.totalInterests())).isEqualByComparingTo("60.06");
        assertThat(Money.round(result.totalCommissions())).isEqualByComparingTo("9.72");
        assertThat(Money.round(result.totalAmountToDisburse())).isEqualByComparingTo("3430.77");
        assertThat(result.disbursementDate()).isEqualTo(DISBURSEMENT_DATE);

        assertThat(result.totalInterests().add(result.totalCommissions()).add(result.totalAmountToDisburse()))
                .isEqualByComparingTo(result.totalAmount());
    }

    private static DocumentModel document(String nominalAmount, LocalDate dueDate) {
        return DocumentModel.builder()
                .documentNumber("DOC-" + nominalAmount)
                .issueDate(DISBURSEMENT_DATE.minusDays(10))
                .dueDate(dueDate)
                .nominalAmount(new BigDecimal(nominalAmount))
                .build();
    }
}
