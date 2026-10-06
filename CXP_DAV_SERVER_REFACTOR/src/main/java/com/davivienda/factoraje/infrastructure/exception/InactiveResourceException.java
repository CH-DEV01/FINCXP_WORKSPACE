package com.davivienda.factoraje.infrastructure.exception;

import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

/**
 * Operación rechazada porque un pagador, proveedor, convenio, línea de crédito o
 * tarifario involucrado no está activo.
 */
public class InactiveResourceException extends ConflictException {

    public InactiveResourceException(String message) {
        super(message);
    }

    public static void requireActive(GeneralStatusEnum status, String message) {
        if (status != GeneralStatusEnum.ACTIVE) {
            throw new InactiveResourceException(message);
        }
    }
}
