package com.davivienda.factoraje.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
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
import jakarta.validation.constraints.DecimalMin;
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
@Table(name = "product_pricing_terms")
public class ProductPricingTermModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tasa de interés aplicada.
     * Escala: Se almacena como fracción decimal no como porcentaje absoluto.
     * Ejemplo: Un interés del 15.5% se debe guardar como 0.1550 (y NO como
     *  15.5000).
     */
    @NotNull(message = "La tasa de interés es obligatoria")
    @DecimalMin(value = "0.0", message = "La tasa de interés no puede ser negativa")
    @Column(name = "interest_rate", precision = 19, scale = 6, nullable = false)
    private BigDecimal interestRate;

    /**
     * Tasa de comisión operativa cobrada por el banco.
     * Escala: Se almacena como fracción decimal no como porcentaje absoluto.
     * Ejemplo: Una comisión del 3% se debe guardar como 0.03.
     */
    @NotNull(message = "La tasa de comisión es obligatoria")
    @DecimalMin(value = "0.0", message = "La tasa de comisión no puede ser negativa")
    @Column(name = "commission_rate", precision = 19, scale = 6, nullable = false)
    private BigDecimal commissionRate;

    @NotNull(message = "La base de cálculo es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_base", nullable = false, length = 50)
    private CalculationBaseEnum calculationBase;

    @NotNull(message = "El estado de la condición de precio es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private GeneralStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_facility_id", nullable = false)
    private CreditFacilityModel creditFacility;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
