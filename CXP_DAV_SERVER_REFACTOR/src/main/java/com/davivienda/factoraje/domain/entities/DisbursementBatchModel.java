package com.davivienda.factoraje.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;

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
import jakarta.validation.constraints.Min;
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
@Table(name = "disbursement_batches")
public class DisbursementBatchModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El número de batch no puede estar vacío")
    @Column(name = "batch_number", nullable = false, length = 255, unique = true)
    private String batchNumber;

    @NotBlank(message = "El nombre del archivo de salida no puede estar vacío")
    @Column(name = "output_file_name", nullable = false, length = 255)
    private String outputFileName;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @NotNull(message = "El monto total del batch es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto total no puede ser negativo")
    @Column(name = "total_amount", precision = 38, scale = 18, nullable = false)
    private BigDecimal totalAmount;

    @NotNull(message = "El monto total de comisiones del batch es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto total de comisiones no puede ser negativo")
    @Column(name = "total_commission", precision = 38, scale = 18, nullable = false)
    private BigDecimal totalCommission;

    @NotNull(message = "El monto total de intereses del batch es obligatorio")
    @DecimalMin(value = "0.0", message = "El monto total de intereses no puede ser negativo")
    @Column(name = "total_interest", precision = 38, scale = 18, nullable = false)
    private BigDecimal totalInterest;

    // PENDING, PROCESSING, COMPLETED, FAILED
    @NotNull(message = "El estado del batch es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DisbursementBatchStatusEnum status;

    // Operador del banco que ejecutó la generación del batch (Relación N:1)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private UserModel createdBy;

    // Operador del banco que confirmó que los desembolsos se ejecutaron (Relación
    // N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_id")
    private UserModel confirmedBy;

    // Cada lote agrupa los documentos de un pagador con la misma fecha de
    // vencimiento y la misma fecha de solicitud de financiamiento.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private EntityModel payer;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}