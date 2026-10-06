package com.davivienda.factoraje.dto.dispersion;

import java.util.UUID;

/**
 * Pagador en la terminal de dispersiones.
 *
 * @param accountNumber  cuenta principal del pagador, a la que se cargan las dispersiones;
 *                       nula si no tiene.
 * @param pendingGroups  fechas de vencimiento con documentos por dispersar.
 */
public record DispersionPayerDTOResponse(
        UUID id,
        String name,
        String accountNumber,
        long pendingGroups
) {}
