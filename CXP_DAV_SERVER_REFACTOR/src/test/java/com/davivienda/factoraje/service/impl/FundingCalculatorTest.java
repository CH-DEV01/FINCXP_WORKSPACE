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
    void documentAmountsAreRoundedOncePerComponentAndTheNetAbsorbsTheDifference() {
        DocumentMathResult result = calculator.calculateDocument(
                document("1000.00", LocalDate.of(2026, 11, 1)), RATES, DISBURSEMENT_DATE);

        assertThat(result.diffDays()).isEqualTo(31);
        assertThat(result.nominalAmount()).isEqualByComparingTo("1000.00");
        // Exacto: interés 10.2276..., que redondeado define el monto a financiar.
        assertThat(result.interest()).isEqualByComparingTo("10.23");
        assertThat(result.amountToFinance()).isEqualByComparingTo("989.77");
        // Comisión sobre 989.77: 2.474425 -> 2.47; IVA sobre 2.47: 0.3211 -> 0.32.
        assertThat(result.commission()).isEqualByComparingTo("2.47");
        assertThat(result.ivaAmount()).isEqualByComparingTo("0.32");
        assertThat(result.commissionWithIva()).isEqualByComparingTo("2.79");
        assertThat(result.disburse()).isEqualByComparingTo("986.98");
        // Las tasas no son dinero: conservan todos sus decimales.
        assertThat(result.discountRate()).isEqualByComparingTo("0.989772352358957440");
        assertThat(result.financingPercentage()).isEqualByComparingTo("0.010227647641042560");

        assertThat(result.interest().add(result.commission()).add(result.ivaAmount()).add(result.disburse()))
                .isEqualByComparingTo(result.nominalAmount());
        assertThat(result.interest().scale()).isEqualTo(Money.SCALE);
        assertThat(result.disburse().scale()).isEqualTo(Money.SCALE);
    }

    @Test
    void commissionWithIvaIsTheSumOfTheShownParts() {
        DocumentMathResult result = calculator.calculateDocument(
                document("1000.00", LocalDate.of(2026, 11, 1)), RATES, DISBURSEMENT_DATE);

        assertThat(result.commission()).isEqualByComparingTo("2.47");
        assertThat(result.ivaAmount()).isEqualByComparingTo("0.32");
        assertThat(result.commissionWithIva()).isEqualByComparingTo("2.79");
    }

    @Test
    void ivaIsCalculatedOnTheRoundedCommission() {
        FundingCalculator.Rates rates = new FundingCalculator.Rates(
                new BigDecimal("0.12"), new BigDecimal("0.005"), 360, new BigDecimal("0.13"));

        DocumentMathResult result = calculator.calculateDocument(
                document("3853.50", DISBURSEMENT_DATE.minusDays(1)), rates, DISBURSEMENT_DATE);

        // 3853.50 * 0.005 = 19.2675 -> 19.27; IVA sobre lo que se cobra: 19.27 * 0.13 = 2.5051 -> 2.51.
        assertThat(result.commission()).isEqualByComparingTo("19.27");
        assertThat(result.ivaAmount()).isEqualByComparingTo("2.51");
        assertThat(result.disburse()).isEqualByComparingTo("3831.72");
    }

    @Test
    void rejectsWhenAmountToDisburseIsNotPositive() {
        FundingCalculator.Rates highCommission = new FundingCalculator.Rates(
                BigDecimal.ZERO, BigDecimal.ONE, 360, new BigDecimal("0.13"));

        assertThatThrownBy(() -> calculator.calculateDocument(
                document("1000.00", LocalDate.of(2026, 11, 1)), highCommission, DISBURSEMENT_DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe ser mayor a cero");
    }

    @Test
    void totalsAreTheSumOfTheDetailInCents() {
        CalculationResult result = calculator.calculate(List.of(
                document("1000.00", LocalDate.of(2026, 11, 1)),
                document("2500.55", LocalDate.of(2026, 12, 1))), RATES, DISBURSEMENT_DATE);

        assertThat(result.details()).hasSize(2);
        DocumentMathResult first = result.details().get(0);
        DocumentMathResult second = result.details().get(1);
        assertThat(first.disburse()).isEqualByComparingTo("986.98");
        assertThat(second.interest()).isEqualByComparingTo("49.83");
        assertThat(second.commission()).isEqualByComparingTo("6.13");
        assertThat(second.ivaAmount()).isEqualByComparingTo("0.80");
        assertThat(second.disburse()).isEqualByComparingTo("2443.79");

        assertThat(result.totalAmount()).isEqualByComparingTo("3500.55");
        assertThat(result.totalAmountToFinance()).isEqualByComparingTo("3440.49");
        assertThat(result.totalInterests()).isEqualByComparingTo("60.06");
        assertThat(result.totalCommissions()).isEqualByComparingTo("9.72");
        assertThat(result.totalAmountToDisburse()).isEqualByComparingTo("3430.77");
        assertThat(result.disbursementDate()).isEqualTo(DISBURSEMENT_DATE);

        // Los totales son la suma literal del detalle: no hay segundo redondeo.
        assertThat(result.totalAmountToDisburse())
                .isEqualByComparingTo(first.disburse().add(second.disburse()));
        assertThat(result.totalInterests())
                .isEqualByComparingTo(first.interest().add(second.interest()));
        assertThat(result.totalCommissions())
                .isEqualByComparingTo(first.commissionWithIva().add(second.commissionWithIva()));
        assertThat(result.totalInterests().add(result.totalCommissions()).add(result.totalAmountToDisburse()))
                .isEqualByComparingTo(result.totalAmount());
        assertThat(Money.round(result.totalAmountToDisburse())).isEqualByComparingTo(result.totalAmountToDisburse());
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
