package com.davivienda.factoraje.dto.document;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InactivateDocumentsDTORequest(
        @NotEmpty(message = "Debe incluir al menos un documento para inactivar.")
        @Size(max = MAX_DOCUMENTS, message = "No se pueden inactivar más de {max} documentos a la vez.")
        List<@NotNull UUID> documentIds
) {
    public static final int MAX_DOCUMENTS = 500;
}
