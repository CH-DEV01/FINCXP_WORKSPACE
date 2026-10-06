package com.davivienda.factoraje.domain.catalogs;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.DisbursementPolicyTypeEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "disbursement_policies_cat")
public class DisbursementPolicyCat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El código de la política de desembolso no puede estar vacío")
    @Column(name = "code", unique = true, nullable = false)
    private String code;

    @NotBlank(message = "El nombre de la política de desembolso no puede estar vacío")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "La descripción de la política de desembolso no puede estar vacía")
    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30)
    private DisbursementPolicyTypeEnum type;

    /** Días de la semana permitidos para WEEKDAYS, separados por coma (p. ej. "MONDAY,THURSDAY"). */
    @Column(name = "weekdays", length = 100)
    private String weekdays;

    @Column(name = "offset_days")
    private Integer offsetDays;

    @NotNull(message = "El status de la política de desembolso no puede estar vacío")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private GeneralStatusEnum status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
