package com.davivienda.factoraje.domain.catalogs;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Opción de menú. {@code path} es absoluto porque un menú puede apuntar fuera de
 * la ruta base del rol (p. ej. /select-agreement).
 */
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "menus_cat", uniqueConstraints = @UniqueConstraint(
        name = "uk_menus_cat_path", columnNames = "path"))
public class MenuCat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "La etiqueta del menú no puede estar vacía")
    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @NotBlank(message = "La ruta del menú no puede estar vacía")
    @Column(name = "path", nullable = false, length = 150)
    private String path;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "description", length = 255)
    private String description;

    @NotNull(message = "El estado del menú no puede estar vacío")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private GeneralStatusEnum status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
