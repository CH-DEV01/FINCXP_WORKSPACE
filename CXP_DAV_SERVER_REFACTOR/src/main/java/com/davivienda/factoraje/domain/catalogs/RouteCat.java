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
 * Pantalla del cliente que puede asignarse a un rol. {@code path} es relativo a la
 * ruta base del rol y {@code componentName} debe existir en el registro de
 * componentes del cliente.
 */
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "routes_cat", uniqueConstraints = @UniqueConstraint(
        name = "uk_routes_cat_path_component", columnNames = { "path", "component_name" }))
public class RouteCat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "La ruta no puede ser nula")
    @Column(name = "path", nullable = false, length = 150)
    private String path;

    @NotBlank(message = "El componente de la ruta no puede estar vacío")
    @Column(name = "component_name", nullable = false, length = 100)
    private String componentName;

    @Column(name = "description", length = 255)
    private String description;

    @NotNull(message = "El estado de la ruta no puede estar vacío")
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
