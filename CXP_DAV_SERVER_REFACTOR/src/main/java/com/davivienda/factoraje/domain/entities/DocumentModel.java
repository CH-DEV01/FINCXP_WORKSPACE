package com.davivienda.factoraje.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.InvoiceTypeEnum;
import com.davivienda.factoraje.domain.enums.IssuanceMethodEnum;
import com.davivienda.factoraje.domain.enums.QuarantineReasonEnum;

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
import jakarta.validation.constraints.PastOrPresent;
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
@Table(name = "documents")
public class DocumentModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "document_number", nullable = true, length = 255)
    private String documentNumber;

    @NotNull(message = "La fecha de emisión es obligatoria")
    @PastOrPresent(message = "La fecha de emisión no puede ser una fecha futura")
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @NotNull(message = "La fecha de vencimiento es obligatoria")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull(message = "El monto nominal es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto nominal de la factura debe ser mayor a cero")
    @Column(name = "nominal_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal nominalAmount;

    @Column(name = "generation_code", nullable = true, length = 36)
    private String generationCode;

    @Column(name = "received_stamp", nullable = true, length = 40)
    private String receivedStamp;

    @Column(name = "control_number", nullable = true, length = 31)
    private String controlNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", length = 30)
    private InvoiceTypeEnum invoiceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "issuance_method", length = 30)
    private IssuanceMethodEnum issuanceMethod;

    @NotNull(message = "El estado del documento es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DocumentStatusEnum status;

    // Se informan al pasar a IN_QUARANTINE o INACTIVATED_BY_PAYER y se conservan si luego se dispersa
    @Enumerated(EnumType.STRING)
    @Column(name = "quarantine_reason", length = 50)
    private QuarantineReasonEnum quarantineReason;

    @Column(name = "quarantined_at")
    private Instant quarantinedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_agreement_id", nullable = false)
    private MasterAgreementModel masterAgreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_batch_id", nullable = false)
    private UploadBatchModel uploadBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispersion_batch_id")
    private DispersionBatchModel dispersionBatch;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
