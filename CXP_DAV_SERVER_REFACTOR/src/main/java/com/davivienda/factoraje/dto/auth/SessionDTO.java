package com.davivienda.factoraje.dto.auth;

/**
 * Vigencia del token de la sesión, en milisegundos desde la época. El cliente no puede
 * leer el JWT de la cookie, así que programa la renovación con estos valores.
 */
public record SessionDTO(long issuedAt, long expiresAt) {
}
