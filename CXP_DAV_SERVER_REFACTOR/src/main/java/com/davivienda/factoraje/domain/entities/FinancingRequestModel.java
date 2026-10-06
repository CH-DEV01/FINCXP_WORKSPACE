package com.davivienda.factoraje.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.FinancingRequestStatusEnum;

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
@Table(name = "financing_requests")
public class FinancingRequestModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El número de solicitud no puede estar vacío")
    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @NotNull(message = "El monto total a financiar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a financiar debe ser mayor a cero")
    @Column(name = "total_amount_to_finance", precision = 38, scale = 18, nullable = false)
    private BigDecimal totalAmountToFinance;

    @NotNull(message = "El monto a desembolsar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a desembolsar debe ser mayor a cero")
    @Column(name = "total_net_amount", precision = 38, scale = 18, nullable = false)
    private BigDecimal totalAmountToBeDisbursed;

    @NotNull(message = "El monto flat es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a flat debe ser mayor a cero")
    @Column(name = "total_flat_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal totalFlatAmount;

    @NotNull(message = "El estado de la solicitud es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FinancingRequestStatusEnum status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private EntityModel supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private UserModel requestedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
