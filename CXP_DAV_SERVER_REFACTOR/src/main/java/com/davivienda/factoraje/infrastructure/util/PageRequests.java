package com.davivienda.factoraje.infrastructure.util;

import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Paginación acotada para los listados: el tamaño se recorta a {@link #MAX_PAGE_SIZE} (sin
 * rechazar, para no romper pantallas que piden más) y el campo de orden debe estar en la
 * lista blanca del endpoint, porque Spring lo traduce a una columna de la consulta.
 */
public final class PageRequests {

    public static final int MAX_PAGE_SIZE = 100;

    private PageRequests() {
    }

    public static Pageable of(int page, int size, String sortBy, String sortDir, Set<String> allowedSortFields) {
        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("No se puede ordenar por \"" + sortBy + "\". Campos permitidos: "
                    + String.join(", ", allowedSortFields.stream().sorted().toList()) + ".");
        }
        Sort.Direction direction = Sort.Direction.DESC.name().equalsIgnoreCase(sortDir)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return of(page, size, Sort.by(direction, sortBy));
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), sort);
    }
}
