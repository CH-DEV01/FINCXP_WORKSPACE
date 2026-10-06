package com.davivienda.factoraje.domain.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

import java.time.Instant;
import java.util.UUID;

/**
 * Todas las cuentas son de Banco Davivienda, por eso no se guarda banco ni tipo de cuenta.
 */
@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "bank_accounts")
public class BankAccountModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "El número de cuenta bancaria no puede estar vacío")
    @Column(name = "account_number", nullable = false, length = 50, unique = true)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entity_id", nullable = false)
    private EntityModel entityModel;

    @Builder.Default
    @Column(name = "is_main", nullable = false)
    private Boolean isMain = false;

    @NotNull(message = "El estatus de la cuenta bancaria no puede estar vacío")
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
