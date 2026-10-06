package com.davivienda.factoraje.dto.term_version;

import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.TermVersionCat;

public record TermVersionDTOResponse(
        UUID id,
        String versionNumber,
        String title,
        String content,
        String acceptanceText,
        String documentUrl,
        LocalDate publicationDate,
        String contentHash
) {
    public static TermVersionDTOResponse fromEntity(TermVersionCat model) {
        return new TermVersionDTOResponse(
                model.getId(),
                model.getVersionNumber(),
                model.getTitle(),
                model.getContent(),
                model.getAcceptanceText(),
                model.getDocumentUrl(),
                model.getPublicationDate(),
                model.getContentHash()
        );
    }
}
