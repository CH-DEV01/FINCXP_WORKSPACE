package com.davivienda.factoraje.domain.enums;
import lombok.Getter;

@Getter
public enum CalculationBaseEnum {

    COMERCIAL_360(360),
    CALENDARIO_365(365);

    private final int days;

    CalculationBaseEnum(int days) {
        this.days = days;
    }

}
