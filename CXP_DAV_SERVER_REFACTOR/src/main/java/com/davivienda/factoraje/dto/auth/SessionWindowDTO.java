package com.davivienda.factoraje.dto.auth;

/** Emisión y vencimiento del token de sesión, en milisegundos desde epoch. */
public record SessionWindowDTO(long issuedAt, long expiresAt) {}
