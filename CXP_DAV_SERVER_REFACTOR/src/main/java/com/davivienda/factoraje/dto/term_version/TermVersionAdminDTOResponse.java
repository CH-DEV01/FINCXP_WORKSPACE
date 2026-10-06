package com.davivienda.factoraje.dto.term_version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.TermVersionStatusEnum;

public record TermVersionAdminDTOResponse(
        UUID id,
        String termTypeCode,
        String versionNumber,
        String title,
        String content,
        String acceptanceText,
        String documentUrl,
        String contentHash,
        TermVersionStatusEnum status,
        LocalDate publicationDate,
        String publishedByName,
        long acceptanceCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static TermVersionAdminDTOResponse fromEntity(TermVersionCat model, long acceptanceCount) {
        UserModel publisher = model.getPublishedBy();
        String publisherName = publisher == null ? null
                : (publisher.getFirstName() + " " + publisher.getLastName()).trim();

        return new TermVersionAdminDTOResponse(
                model.getId(),
                model.getTermType().getUniqueCode(),
                model.getVersionNumber(),
                model.getTitle(),
                model.getContent(),
                model.getAcceptanceText(),
                model.getDocumentUrl(),
                model.getContentHash(),
                model.getStatus(),
                model.getPublicationDate(),
                publisherName,
                acceptanceCount,
                model.getCreatedAt(),
                model.getUpdatedAt()
        );
    }
}
