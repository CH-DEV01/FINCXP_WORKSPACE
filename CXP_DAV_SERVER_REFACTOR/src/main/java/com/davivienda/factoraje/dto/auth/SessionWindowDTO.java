package com.davivienda.factoraje.dto.auth;

/**
 * Emisión y vencimiento del token de sesión, en milisegundos desde epoch.
 * {@code serverTime} es la hora del nodo al armar la respuesta, para que el cliente
 * corrija el desfase de su reloj local al programar renovación y cierre.
 */
public record SessionWindowDTO(long issuedAt, long expiresAt, long serverTime) {}
