package com.davivienda.factoraje.domain.entities;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.davivienda.factoraje.domain.catalogs.RoleCat;
import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

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
@Table(name = "users")
public class UserModel implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El primer nombre del usuario no puede estar vacío")
    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @NotBlank(message = "El apellido del usuario no puede estar vacío")
    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @NotBlank(message = "El email del usuario no puede estar vacío")
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank(message = "El documento único de identidad (DUI) no puede estar vacío")
    @Column(name = "dui", nullable = false, unique = true, length = 15)
    private String dui;

    @NotNull(message = "El estado del usuario es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private GeneralStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entity_id", nullable = false)
    private EntityModel entity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleCat role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** "Nombre Apellido", omitiendo las partes vacías. */
    public String fullName() {
        return Stream.of(this.firstName, this.lastName)
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }

    // =========================================================================
    // MÉTODOS DE LA INTERFAZ UserDetails (SPRING SECURITY)
    // =========================================================================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (this.role.getStatus() != GeneralStatusEnum.ACTIVE) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority(Roles.PREFIX + this.role.getName()));
    }

    @Override
    public String getUsername() {
        // El "username" de Spring Security es el DUI: el SSO identifica al usuario por él.
        return this.dui;
    }

    @Override
    public String getPassword() {
        return ""; // Sin contraseña local: la autenticación la hace el SSO.
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * También exige la entidad activa: el filtro JWT lo evalúa en cada petición, así que
     * inactivar un pagador o proveedor corta las sesiones abiertas de sus usuarios.
     */
    @Override
    public boolean isEnabled() {
        return this.status == GeneralStatusEnum.ACTIVE
                && this.entity != null
                && this.entity.getStatus() == GeneralStatusEnum.ACTIVE;
    }

}
