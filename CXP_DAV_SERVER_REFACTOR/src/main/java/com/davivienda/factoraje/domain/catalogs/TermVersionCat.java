package com.davivienda.factoraje.domain.catalogs;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.TermVersionStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Versión del texto legal de un tipo de término. Solo los borradores (DRAFT) son
 * editables; al publicarse quedan inmutables porque las aceptaciones registradas
 * en acceptance_audits apuntan a ellas.
 *
 * Las restricciones NOT NULL de title, content y acceptance_text están en la
 * migración V1 de Flyway.
 */
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "term_versions_cat")
public class TermVersionCat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El número de versión del término no puede estar vacío")
    @Column(name = "version_number", nullable = false)
    private String versionNumber;

    @NotBlank(message = "El título del término no puede estar vacío")
    @Column(name = "title", length = 255)
    private String title;

    /** Texto legal en Markdown; puede contener marcadores {{VARIABLE}} que resuelve el cliente. */
    @NotBlank(message = "El contenido del término no puede estar vacío")
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @NotBlank(message = "El texto de aceptación no puede estar vacío")
    @Column(name = "acceptance_text", length = 500)
    private String acceptanceText;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    /** SHA-256 (hex) de title, content y acceptance_text; se calcula al publicar. */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @PastOrPresent(message = "La fecha de publicación no puede ser una fecha futura")
    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_id")
    private UserModel publishedBy;

    @NotNull(message = "El estado de la versión del término es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TermVersionStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_type_id", nullable = false)
    private TermTypeCat termType;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
