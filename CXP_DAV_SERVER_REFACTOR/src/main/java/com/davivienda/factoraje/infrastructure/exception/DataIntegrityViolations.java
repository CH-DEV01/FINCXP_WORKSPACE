package com.davivienda.factoraje.infrastructure.exception;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;

/**
 * Distingue desbordamientos de columna (SQLSTATE 22xxx) de conflictos de unicidad
 * u otras violaciones de integridad (VAL-01).
 */
public final class DataIntegrityViolations {

    public static final String VALUE_OUT_OF_RANGE_MESSAGE =
            "Uno o más valores superan el tamaño o la precisión permitidos. Revise longitudes y montos e intente de nuevo.";

    public static final String CONCURRENT_WRITE_MESSAGE =
            "Otra operación registró los mismos datos al mismo tiempo. Actualice la información e intente de nuevo.";

    private DataIntegrityViolations() {
    }

    public static boolean isValueOutOfRange(Throwable error) {
        String sqlState = sqlStateOf(error);
        return "22001".equals(sqlState) || "22003".equals(sqlState);
    }

    public static String clientMessage(DataIntegrityViolationException error) {
        return isValueOutOfRange(error) ? VALUE_OUT_OF_RANGE_MESSAGE : CONCURRENT_WRITE_MESSAGE;
    }

    static String sqlStateOf(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
