package com.davivienda.factoraje.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;

public interface FinancingTransactionRepository extends JpaRepository<FinancingTransactionModel, UUID> {

    List<FinancingTransactionModel> findByDocument_IdIn(List<UUID> documentIds);
    @Query("""
        SELECT tx FROM FinancingTransactionModel tx
        JOIN FETCH tx.document d
        JOIN FETCH d.masterAgreement ma
        JOIN FETCH ma.supplier
        JOIN FETCH ma.payer
        WHERE tx.disbursementBatch.id = :batchId
        ORDER BY d.dueDate ASC, d.documentNumber ASC
    """)
    List<FinancingTransactionModel> findByDisbursementBatchIdWithDocuments(@Param("batchId") UUID batchId);

    /**
     * Totales por proveedor de cada combinación (vencimiento, solicitud, desembolso)
     * pendiente de lote: [vencimiento, solicitud, desembolso, id del proveedor,
     * documentos, suma nominal, suma del monto a desembolsar].
     */
    @Query("""
        SELECT d.dueDate, tx.createdAt, tx.scheduledDisbursementDate, ma.supplier.id,
            COUNT(tx), SUM(d.nominalAmount), SUM(tx.amountToFinance)
        FROM FinancingTransactionModel tx
        JOIN tx.document d
        JOIN d.masterAgreement ma
        WHERE d.status = :status
        AND ma.payer.id = :payerId
        GROUP BY d.dueDate, tx.createdAt, tx.scheduledDisbursementDate, ma.supplier.id
        ORDER BY tx.scheduledDisbursementDate ASC, d.dueDate ASC, tx.createdAt ASC
    """)
    List<Object[]> sumPendingGroupsBySupplier(
            @Param("payerId") UUID payerId,
            @Param("status") DocumentStatusEnum status);

    /** Combinaciones pendientes de lote de todos los pagadores: [id del pagador, vencimiento, solicitud, desembolso]. */
    @Query("""
        SELECT DISTINCT ma.payer.id, d.dueDate, tx.createdAt, tx.scheduledDisbursementDate
        FROM FinancingTransactionModel tx
        JOIN tx.document d
        JOIN d.masterAgreement ma
        WHERE d.status = :status
    """)
    List<Object[]> findPendingGroupsOfAllPayers(@Param("status") DocumentStatusEnum status);

    /** [id del lote, id del proveedor, suma del monto a desembolsar] de los lotes del pagador. */
    @Query("""
        SELECT tx.disbursementBatch.id, ma.supplier.id, SUM(tx.amountToFinance)
        FROM FinancingTransactionModel tx
        JOIN tx.document d
        JOIN d.masterAgreement ma
        WHERE tx.disbursementBatch.payer.id = :payerId
        GROUP BY tx.disbursementBatch.id, ma.supplier.id
    """)
    List<Object[]> sumBatchesBySupplierForPayer(@Param("payerId") UUID payerId);

    /** [id del lote, id del proveedor, suma del monto a desembolsar] de los lotes indicados. */
    @Query("""
        SELECT tx.disbursementBatch.id, ma.supplier.id, SUM(tx.amountToFinance)
        FROM FinancingTransactionModel tx
        JOIN tx.document d
        JOIN d.masterAgreement ma
        WHERE tx.disbursementBatch.id IN :batchIds
        GROUP BY tx.disbursementBatch.id, ma.supplier.id
    """)
    List<Object[]> sumBatchesBySupplier(@Param("batchIds") Collection<UUID> batchIds);

    /**
     * Monto a desembolsar de todos los lotes, redondeado por lote y proveedor como
     * en la carta para que el total cuadre con la suma de los lotes.
     */
    @Query("""
        SELECT COALESCE(SUM(ROUND(t.amount, 2)), 0)
        FROM (
            SELECT SUM(tx.amountToFinance) AS amount
            FROM FinancingTransactionModel tx
            JOIN tx.document d
            JOIN d.masterAgreement ma
            WHERE tx.disbursementBatch IS NOT NULL
            GROUP BY tx.disbursementBatch.id, ma.supplier.id
        ) t
    """)
    BigDecimal sumAmountToDisburseOfAllBatches();

    /** Igual que {@link #sumAmountToDisburseOfAllBatches()}, sólo con los lotes del pagador. */
    @Query("""
        SELECT COALESCE(SUM(ROUND(t.amount, 2)), 0)
        FROM (
            SELECT SUM(tx.amountToFinance) AS amount
            FROM FinancingTransactionModel tx
            JOIN tx.document d
            JOIN d.masterAgreement ma
            WHERE tx.disbursementBatch.payer.id = :payerId
            GROUP BY tx.disbursementBatch.id, ma.supplier.id
        ) t
    """)
    BigDecimal sumAmountToDisburseOfPayerBatches(@Param("payerId") UUID payerId);

    /** Transacciones pendientes de lote de una combinación (vencimiento, solicitud, desembolso). */
    @Query("""
        SELECT tx FROM FinancingTransactionModel tx
        JOIN FETCH tx.document d
        JOIN FETCH d.masterAgreement ma
        JOIN FETCH ma.supplier
        WHERE d.status = :status
        AND ma.payer.id = :payerId
        AND d.dueDate = :dueDate
        AND tx.createdAt = :requestDate
        AND tx.scheduledDisbursementDate = :disbursementDate
        ORDER BY d.documentNumber ASC
    """)
    List<FinancingTransactionModel> findPendingInGroup(
            @Param("payerId") UUID payerId,
            @Param("status") DocumentStatusEnum status,
            @Param("dueDate") LocalDate dueDate,
            @Param("requestDate") LocalDate requestDate,
            @Param("disbursementDate") LocalDate disbursementDate);

}
