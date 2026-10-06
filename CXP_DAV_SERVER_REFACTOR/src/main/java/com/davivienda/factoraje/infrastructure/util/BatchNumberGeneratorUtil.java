package com.davivienda.factoraje.infrastructure.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.davivienda.factoraje.domain.enums.BatchTypeEnum;

public final class BatchNumberGeneratorUtil {

    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyyMMdd");

    private BatchNumberGeneratorUtil() {
    }

    /** Prefijo, fecha y código aleatorio de 6 caracteres. Ejemplo: INV-20260917-A8B9C1. */
    public static String generateBatchNumber(BatchTypeEnum batchType, LocalDate date) {
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return batchType.getPrefix() + "-" + date.format(DATE_PART) + "-" + randomPart;
    }
}
