package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.infrastructure.util.Money;

import lombok.extern.slf4j.Slf4j;

/** Cálculo financiero del factoraje, sin estado ni acceso a datos. */
@Slf4j
@Component
public class FundingCalculator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record Rates(BigDecimal interestRate, BigDecimal commissionRate, int baseDays, BigDecimal ivaRate) {}

    public CalculationResult calculate(List<DocumentModel> documents, Rates rates, LocalDate disbursementDate) {

        List<DocumentMathResult> details = new ArrayList<>();
        BigDecimal totalAmountToFinance = Money.ZERO;
        BigDecimal totalAmount = Money.ZERO;
        BigDecimal totalInterests = Money.ZERO;
        BigDecimal totalCommissions = Money.ZERO;
        BigDecimal totalAmountToDisburse = Money.ZERO;

        for (DocumentModel document : documents) {
            DocumentMathResult docMath = calculateDocument(document, rates, disbursementDate);
            details.add(docMath);

            totalAmountToFinance = totalAmountToFinance.add(docMath.amountToFinance());
            totalAmount = totalAmount.add(docMath.nominalAmount());
            totalInterests = totalInterests.add(docMath.interest());
            totalCommissions = totalCommissions.add(docMath.commissionWithIva());
            totalAmountToDisburse = totalAmountToDisburse.add(docMath.disburse());
        }

        log.info("[CALCULO] Totales | nominal {} | monto a financiar {} | interés {} | comisión con IVA {}"
                        + " | a desembolsar {}",
                totalAmount, totalAmountToFinance, totalInterests, totalCommissions, totalAmountToDisburse);

        return new CalculationResult(
                details, totalAmount, totalAmountToFinance, totalInterests, totalCommissions, totalAmountToDisburse,
                disbursementDate);
    }

    public DocumentMathResult calculateDocument(DocumentModel document, Rates rates, LocalDate disbursementDate) {

        LocalDate issueDate = document.getIssueDate();
        LocalDate dueDate = document.getDueDate();

        int diffDays = financingDays(disbursementDate, dueDate);

        BigDecimal nominalAmount = document.getNominalAmount();

        BigDecimal discountFactor = calculateDiscountFactor(rates.interestRate(), rates.baseDays(), diffDays);

        // Regla única de redondeo: cada componente se redondea a centavos aquí, una sola vez,
        // como en una factura. El neto absorbe la diferencia, así que
        // interés + comisión + IVA + neto == nominal exactamente, y todo total (simulación,
        // lote, carta) es una simple suma de estos centavos. Las tasas (factor y porcentaje)
        // se conservan con todos sus decimales porque no son dinero.
        BigDecimal interest = Money.round(
                nominalAmount.subtract(nominalAmount.multiply(discountFactor, Money.CALC)));
        BigDecimal amountToFinance = nominalAmount.subtract(interest);

        // La comisión se cobra sobre el monto a financiar (nominal menos intereses); el IVA
        // se calcula sobre la comisión ya redondeada, que es la que se cobra.
        BigDecimal pureCommission = Money.round(amountToFinance.multiply(rates.commissionRate(), Money.CALC));
        BigDecimal ivaAmount = Money.round(pureCommission.multiply(rates.ivaRate(), Money.CALC));
        BigDecimal totalCommissionWithIva = pureCommission.add(ivaAmount);

        BigDecimal disburse = nominalAmount.subtract(interest).subtract(totalCommissionWithIva);
        if (disburse.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(String.format(
                    "El monto a desembolsar del documento %s resulta $%s; debe ser mayor a cero. "
                            + "Revise la tasa de interés, la comisión o el IVA configurados.",
                    document.displayNumber(), Money.roundOrZero(disburse)));
        }

        log.debug("[CALCULO] Doc {} | vence {} | días = ({} - {}) + 1 = {} | nominal {}"
                        + " | factor = 1 / (1 + ({} / {}) * {}) = {}"
                        + " | monto a financiar = nominal * factor = {} | interés = nominal - monto a financiar = {}"
                        + " | comisión = monto a financiar * {} = {} | IVA = comisión * {} = {} | comisión con IVA = {}"
                        + " | a desembolsar = nominal - interés - comisión con IVA = {}",
                document.displayNumber(), DATE_FMT.format(dueDate),
                DATE_FMT.format(dueDate), DATE_FMT.format(disbursementDate), diffDays, nominalAmount,
                rates.interestRate(), rates.baseDays(), diffDays, discountFactor,
                amountToFinance, interest,
                rates.commissionRate(), pureCommission, rates.ivaRate(), ivaAmount, totalCommissionWithIva,
                disburse);

        return new DocumentMathResult(
                document, issueDate, dueDate, diffDays, nominalAmount, amountToFinance,
                interest, pureCommission, ivaAmount, discountFactor,
                BigDecimal.ONE.subtract(discountFactor),
                disburse);
    }

    /** Conteo inclusivo: cuentan tanto el día de desembolso como el de vencimiento. */
    public int financingDays(LocalDate disbursementDate, LocalDate dueDate) {
        return (int) ChronoUnit.DAYS.between(disbursementDate, dueDate) + 1;
    }

    /**
     * Calcula el factor de descuento (porcentaje a aplicar sobre el valor nominal)
     * Fórmula: 1 / (1 + (Tasa / Base) * Días)
     */
    public BigDecimal calculateDiscountFactor(BigDecimal interestRate, int baseDays, int diffDays) {
        if (baseDays == 0) {
            log.error("División por cero evitada: la base no puede ser cero.");
            throw new IllegalArgumentException("La base para el cálculo no puede ser cero.");
        }

        if (diffDays <= 0) {
            return BigDecimal.ONE;
        }

        BigDecimal dailyRate = interestRate.divide(BigDecimal.valueOf(baseDays), Money.CALC);
        BigDecimal divisor = BigDecimal.ONE.add(dailyRate.multiply(BigDecimal.valueOf(diffDays), Money.CALC));

        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return Money.exact(BigDecimal.ONE.divide(divisor, Money.CALC));
    }

    public record CalculationResult(
            List<DocumentMathResult> details,
            BigDecimal totalAmount,
            BigDecimal totalAmountToFinance,
            BigDecimal totalInterests,
            BigDecimal totalCommissions,
            BigDecimal totalAmountToDisburse,
            LocalDate disbursementDate) {
    }

    public record DocumentMathResult(
            DocumentModel document,
            LocalDate issueDate,
            LocalDate dueDate,
            Integer diffDays,
            BigDecimal nominalAmount,
            BigDecimal amountToFinance,
            BigDecimal interest,
            BigDecimal commission,
            BigDecimal ivaAmount,
            BigDecimal discountRate,
            BigDecimal financingPercentage,
            BigDecimal disburse) {

        public BigDecimal commissionWithIva() {
            return commission.add(ivaAmount);
        }
    }
}
