package com.davivienda.factoraje.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.UploadResourceModel;
import com.davivienda.factoraje.domain.enums.UploadResourceType;

@Repository
public interface UploadResourceRepository extends JpaRepository<UploadResourceModel, UUID> {

    Optional<UploadResourceModel> findByType(UploadResourceType type);

    /** Datos para mostrar los recursos sin leer el contenido de los archivos. */
    @Query("""
            SELECT r.type AS type, r.fileName AS fileName, r.fileSize AS fileSize,
                   r.updatedAt AS updatedAt, u.firstName AS updatedByFirstName, u.lastName AS updatedByLastName
            FROM UploadResourceModel r LEFT JOIN r.updatedBy u
            """)
    List<Summary> findSummaries();

    interface Summary {
        UploadResourceType getType();

        String getFileName();

        Long getFileSize();

        Instant getUpdatedAt();

        String getUpdatedByFirstName();

        String getUpdatedByLastName();
    }
}
