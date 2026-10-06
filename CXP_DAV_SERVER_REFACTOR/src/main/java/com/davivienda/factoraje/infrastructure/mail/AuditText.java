package com.davivienda.factoraje.infrastructure.mail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.infrastructure.util.Money;

/** Textos del valor anterior y nuevo del aviso de auditoría. */
public final class AuditText {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private AuditText() {
    }

    public static String status(GeneralStatusEnum status) {
        if (status == null) {
            return "—";
        }
        return status == GeneralStatusEnum.ACTIVE ? "Activo" : "Inactivo";
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }

    public static String amount(BigDecimal amount) {
        return amount == null ? "—" : Money.format(amount);
    }

    public static String value(Object value) {
        if (value == null) {
            return "—";
        }
        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }
        String text = value.toString();
        return text.isBlank() ? "—" : text;
    }
}
