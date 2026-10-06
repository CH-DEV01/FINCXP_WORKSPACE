package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestSupplierDTOResponse;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;

import lombok.RequiredArgsConstructor;

/** Agrupaciones y montos de desembolso compartidos por las consultas, la carta y la confirmación. */
@Component
@RequiredArgsConstructor
public class DisbursementSummaries {

    private final BankAccountRepository bankAccountRepository;
    private final DisbursementPolicyService disbursementPolicyService;

    /** Resumen por proveedor: el mismo que ve el operador en el detalle y en el PDF del lote. */
    public List<DisbursementRequestSupplierDTOResponse> summarizeBySupplier(
            List<FinancingTransactionModel> transactions) {

        Map<UUID, List<FinancingTransactionModel>> bySupplier = groupBySupplier(transactions);

        if (bySupplier.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> accountBySupplier = new HashMap<>();
        bankAccountRepository.findAllByEntityModelIdInAndIsMainTrue(bySupplier.keySet())
                .forEach(account -> accountBySupplier.putIfAbsent(
                        account.getEntityModel().getId(), account.getAccountNumber()));

        return bySupplier.entrySet().stream()
                .map(entry -> {
                    List<FinancingTransactionModel> txs = entry.getValue();
                    DocumentModel first = txs.getFirst().getDocument();
                    return new DisbursementRequestSupplierDTOResponse(
                            entry.getKey(),
                            first.getMasterAgreement().getSupplier().getName(),
                            accountBySupplier.get(entry.getKey()),
                            txs.size(),
                            sumCents(txs, FinancingTransactionModel::getAmountToFinance),
                            sumCents(txs, FinancingTransactionModel::getAmountToBeDisbursed),
                            first.getDueDate());
                })
                .sorted(Comparator.comparing(DisbursementRequestSupplierDTOResponse::supplierName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    /**
     * Fecha de desembolso programada al momento de la solicitud. Los lotes creados
     * antes de guardarla en el lote la toman de sus transacciones.
     */
    public LocalDate disbursementDate(DisbursementBatchModel batch, List<FinancingTransactionModel> transactions) {
        if (batch.getDisbursementDate() != null) {
            return batch.getDisbursementDate();
        }
        if (!transactions.isEmpty() && transactions.getFirst().getScheduledDisbursementDate() != null) {
            return transactions.getFirst().getScheduledDisbursementDate();
        }
        return batch.getCreatedAt() != null
                ? LocalDate.ofInstant(batch.getCreatedAt(), disbursementPolicyService.businessZone())
                : disbursementPolicyService.businessToday();
    }

    public static Map<UUID, List<FinancingTransactionModel>> groupBySupplier(
            List<FinancingTransactionModel> transactions) {
        Map<UUID, List<FinancingTransactionModel>> bySupplier = new LinkedHashMap<>();
        for (FinancingTransactionModel tx : transactions) {
            UUID supplierId = tx.getDocument().getMasterAgreement().getSupplier().getId();
            bySupplier.computeIfAbsent(supplierId, id -> new ArrayList<>()).add(tx);
        }
        return bySupplier;
    }

    public static BigDecimal sumCents(List<FinancingTransactionModel> txs,
            Function<FinancingTransactionModel, BigDecimal> amount) {
        return Money.round(txs.stream()
                .map(amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Redondeo por proveedor que usa la carta, para que los totales cuadren con ella. */
    public static BigDecimal cents(BigDecimal amount) {
        return Money.roundOrZero(amount);
    }
}
