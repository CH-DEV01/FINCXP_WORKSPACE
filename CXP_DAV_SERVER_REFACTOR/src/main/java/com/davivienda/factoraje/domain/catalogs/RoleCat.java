package com.davivienda.factoraje.domain.catalogs;

import java.time.Instant;
import java.util.Objects;
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

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles_cat")
public class RoleCat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El nombre del rol no puede estar vacío")
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @NotBlank(message = "La descripción del rol no puede estar vacía")
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    /** Ruta base del layout del rol (p. ej. /admin); sus rutas se montan debajo. */
    @Column(name = "default_route", length = 100)
    private String defaultRoute;

    @NotNull(message = "El estado del rol no puede estar vacío")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private GeneralStatusEnum status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RoleCat that = (RoleCat) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
