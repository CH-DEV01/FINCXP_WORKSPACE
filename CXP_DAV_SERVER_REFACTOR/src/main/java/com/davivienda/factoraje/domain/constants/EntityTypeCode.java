package com.davivienda.factoraje.domain.constants;

import java.util.Map;

/**
 * Códigos de {@code entity_type_cat}. Son constantes y no un enum porque se usan
 * como etiquetas de {@code switch} y en consultas por código.
 */
public final class EntityTypeCode {

    public static final String PAYER = "COD_001";
    public static final String SUPPLIER = "COD_002";
    public static final String BANK = "COD_003";

    private static final Map<String, String> BY_ROLE = Map.of(
            Roles.ADMIN, BANK,
            Roles.SYSTEM_ADMIN, BANK,
            Roles.PAYER, PAYER,
            Roles.SUPPLIER, SUPPLIER);

    private EntityTypeCode() {
    }

    /** Tipo de entidad al que debe pertenecer un usuario con el rol; {@code null} si el rol no tiene uno. */
    public static String forRole(String roleName) {
        return roleName != null ? BY_ROLE.get(roleName) : null;
    }
}
