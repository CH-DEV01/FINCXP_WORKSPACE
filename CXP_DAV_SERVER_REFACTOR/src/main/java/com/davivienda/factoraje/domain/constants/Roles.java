package com.davivienda.factoraje.domain.constants;

/**
 * Nombres de {@code roles_cat}. Spring Security los recibe como autoridades con
 * el prefijo {@link #PREFIX}.
 */
public final class Roles {

    public static final String PREFIX = "ROLE_";

    public static final String ADMIN = "ADMIN";
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String PAYER = "PAYER";
    public static final String SUPPLIER = "SUPPLIER";

    private Roles() {
    }
}
