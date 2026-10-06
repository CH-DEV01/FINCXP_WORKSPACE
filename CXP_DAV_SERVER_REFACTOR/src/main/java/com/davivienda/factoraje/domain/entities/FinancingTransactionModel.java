package com.davivienda.factoraje.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "financing_transactions")
public class FinancingTransactionModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "El monto de intereses es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto de intereses no puede ser negativo")
    @Column(name = "interest_amount", precision = 38, scale = 18, nullable = false)
    private BigDecimal interestAmount;

    @NotNull(message = "El monto de comisiones es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto de comisiones no puede ser negativo")
    @Column(name = "commission_amount", precision = 38, scale = 18, nullable = false)
    private BigDecimal commissionAmount;

    @NotNull(message = "El monto a financiar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a financiar debe ser mayor a cero")
    @Column(name = "amount_to_finance", precision = 38, scale = 18, nullable = false)
    private BigDecimal amountToFinance;

    @NotNull(message = "El monto flat es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto flat debe ser mayor a cero")
    @Column(name = "flat_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal flatAmount;

    @NotNull(message = "El monto a desembolsar es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto a financiar debe ser mayor a cero")
    @Column(name = "amount_to_be_disbursed", precision = 38, scale = 18, nullable = false)
    private BigDecimal amountToBeDisbursed;

    @NotNull(message = "La tasa de descuento es obligatoria")
    @DecimalMin(value = "0.0", message = "La tasa de descuento no puede ser negativa")
    @Column(name = "discount_rate", precision = 38, scale = 18, nullable = false)
    private BigDecimal discountRate;

    @NotNull(message = "El monto de IVA es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto de IVA no puede ser negativo")
    @Column(name = "iva_amount", precision = 38, scale = 18, nullable = false)
    private BigDecimal ivaAmount;

    @NotNull(message = "El porcentaje de financiamiento es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje de financiamiento no puede ser negativo")
    @Column(name = "financing_percentage", precision = 38, scale = 18, nullable = false)
    private BigDecimal financingPercentage;

    @NotNull(message = "La fecha planificada de desembolso es obligatoria")
    @Column(name = "scheduled_disbursement_date", nullable = false)
    private LocalDate scheduledDisbursementDate;

    // Relación 1:1 con el documento (factura) de origen
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private DocumentModel document;

    // Relación N:1 con la solicitud de financiamiento
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "financing_request_id", nullable = false)
    private FinancingRequestModel financingRequest;

    // Relación N:1 con el lote de desembolso.
    // Es opcional (nullable = true) porque al principio nace como null.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursement_batch_id")
    private DisbursementBatchModel disbursementBatch;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDate createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
