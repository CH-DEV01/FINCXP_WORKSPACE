package com.davivienda.factoraje.dto.auth;

public record RouteDTO(
    String path,
    String componentName,
    boolean isIndex // Para saber si es la ruta por defecto (index) del Layout
) {}
