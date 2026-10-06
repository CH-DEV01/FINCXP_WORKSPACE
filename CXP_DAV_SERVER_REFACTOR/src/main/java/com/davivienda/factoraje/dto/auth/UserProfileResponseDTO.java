package com.davivienda.factoraje.dto.auth;

import java.util.List;
import java.util.UUID;

public record UserProfileResponseDTO(
    UUID id,
    String dui,
    String firstName,
    String lastName,
    String email,
    String role,
    String defaultRoute,
    UUID entityId,
    String entityName,
    List<String> permissions,
    List<MenuDTO> menus,
    List<RouteDTO> routes,
    int idleTimeoutMinutes
) {}
