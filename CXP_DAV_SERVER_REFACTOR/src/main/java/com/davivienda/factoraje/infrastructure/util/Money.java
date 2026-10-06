package com.davivienda.factoraje.infrastructure.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Montos monetarios. Los cálculos trabajan con todos los decimales ({@link #CALC}) y
 * se guardan con {@link #STORAGE_SCALE}; solo al mostrarlos se redondean a 2 decimales
 * HALF_UP con formato {@code $#,##0.00}, como una celda con formato en una hoja de cálculo.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    public static final MathContext CALC = MathContext.DECIMAL128;
    /** Debe coincidir con la escala de las columnas calculadas (migración V3). */
    public static final int STORAGE_SCALE = 18;

    private static final String PATTERN = "$#,##0.00";

    private Money() {
    }

    public static BigDecimal round(BigDecimal amount) {
        return amount == null ? null : amount.setScale(SCALE, ROUNDING);
    }

    public static BigDecimal roundOrZero(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(SCALE, ROUNDING);
    }

    /** Valor de cálculo con la escala con que se guarda, para que lo guardado sea lo calculado. */
    public static BigDecimal exact(BigDecimal amount) {
        return amount == null ? null : amount.setScale(STORAGE_SCALE, ROUNDING);
    }

    /** DecimalFormat no es thread-safe, por eso se crea una instancia por llamada. */
    public static String format(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat(PATTERN, DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(ROUNDING);
        return format.format(roundOrZero(amount));
    }

    /** Monto con 2 decimales y sin símbolo ni separador de miles, p. ej. {@code 1234.50}. */
    public static String plain(BigDecimal amount) {
        return roundOrZero(amount).toPlainString();
    }

    public static boolean hasMoreThanTwoDecimals(BigDecimal amount) {
        return amount != null && amount.stripTrailingZeros().scale() > SCALE;
    }
}
