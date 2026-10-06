package com.davivienda.factoraje.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.infrastructure.util.Money;

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
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
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
@Table(name = "credit_facilities")
public class CreditFacilityModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El número de la facilidad de crédito es obligatorio")
    @Column(name = "credit_facility_number", nullable = false, length = 255)
    private String creditFacilityNumber;

    @NotNull(message = "El límite de la facilidad de crédito es obligatorio")
    @DecimalMin(value = "0.0", message = "El límite de la facilidad de crédito no puede ser negativo")
    @Column(name = "facility_limit_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal facilityLimitAmount;

    @Builder.Default
    @NotNull(message = "El monto en uso de la facilidad de crédito es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto en uso de la facilidad de crédito no puede ser negativo")
    @Column(name = "amount_in_use", precision = 19, scale = 4, nullable = false)
    private BigDecimal amountInUse = BigDecimal.ZERO;

    @NotNull(message = "El estado de la facilidad de crédito es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private GeneralStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_id", nullable = false)
    private EntityModel payer;

    @Column(name = "warning_threshold_percentage", precision = 5, scale = 2)
    private BigDecimal warningThresholdPercentage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Getter(AccessLevel.NONE)
    @Transient
    private BigDecimal availableAmount;

    public BigDecimal getAvailableAmount() {
        if (this.facilityLimitAmount == null || this.amountInUse == null) {
            return BigDecimal.ZERO;
        }
        return this.facilityLimitAmount.subtract(this.amountInUse);
    }

    /**
     * Uso máximo que admite una carga de documentos: límite × umbral de alerta.
     *
     * @param defaultThreshold fracción (0.80 = 80%) usada si la línea no tiene umbral propio.
     */
    public BigDecimal uploadLimitAmount(BigDecimal defaultThreshold) {
        BigDecimal threshold = this.warningThresholdPercentage != null ? this.warningThresholdPercentage : defaultThreshold;
        return Money.round(this.facilityLimitAmount.multiply(threshold));
    }

    /** Monto que aún puede cargarse sin superar {@link #uploadLimitAmount}; nunca negativo. */
    public BigDecimal availableToUpload(BigDecimal defaultThreshold) {
        BigDecimal inUse = this.amountInUse != null ? this.amountInUse : BigDecimal.ZERO;
        return uploadLimitAmount(defaultThreshold).subtract(inUse).max(BigDecimal.ZERO);
    }

}
