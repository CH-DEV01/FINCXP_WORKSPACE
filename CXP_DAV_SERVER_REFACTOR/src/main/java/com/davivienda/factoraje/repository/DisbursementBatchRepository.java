package com.davivienda.factoraje.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.dto.credit_facility.RestorableDisbursementBatchDTOResponse;

import jakarta.persistence.LockModeType;

@Repository 
public interface DisbursementBatchRepository extends JpaRepository<DisbursementBatchModel, UUID> {

    boolean existsByBatchNumber(String batchNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DisbursementBatchModel b WHERE b.id = :id")
    Optional<DisbursementBatchModel> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
        SELECT b FROM DisbursementBatchModel b
        JOIN FETCH b.createdBy
        LEFT JOIN FETCH b.confirmedBy
        LEFT JOIN FETCH b.payer
    """, countQuery = "SELECT COUNT(b) FROM DisbursementBatchModel b")
    Page<DisbursementBatchModel> findPageWithUsers(Pageable pageable);

    @Query(value = """
        SELECT b FROM DisbursementBatchModel b
        JOIN FETCH b.createdBy
        LEFT JOIN FETCH b.confirmedBy
        JOIN FETCH b.payer p
        WHERE p.id = :payerId
    """, countQuery = "SELECT COUNT(b) FROM DisbursementBatchModel b WHERE b.payer.id = :payerId")
    Page<DisbursementBatchModel> findPageByPayerWithUsers(@Param("payerId") UUID payerId, Pageable pageable);

    @Query("""
        SELECT b FROM DisbursementBatchModel b
        JOIN FETCH b.createdBy
        LEFT JOIN FETCH b.confirmedBy
        JOIN FETCH b.payer p
        WHERE p.id = :payerId
        ORDER BY b.createdAt DESC
    """)
    List<DisbursementBatchModel> findAllByPayerWithUsersOrderByCreatedAtDesc(@Param("payerId") UUID payerId);

    /** Lotes liquidados del pagador que todavía no se han abonado al cupo. */
    @Query("""
        SELECT new com.davivienda.factoraje.dto.credit_facility.RestorableDisbursementBatchDTOResponse(
            b.id, b.batchNumber, b.dueDate, b.disbursementDate, COUNT(t), SUM(d.nominalAmount), b.totalAmount)
        FROM FinancingTransactionModel t
        JOIN t.disbursementBatch b
        JOIN t.document d
        WHERE b.payer.id = :payerId
          AND b.status = com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum.SETTLED
          AND NOT EXISTS (SELECT h.id FROM CreditFacilityHistoryModel h WHERE h.disbursementBatch = b)
        GROUP BY b.id, b.batchNumber, b.dueDate, b.disbursementDate, b.totalAmount
        ORDER BY b.dueDate, b.batchNumber
    """)
    List<RestorableDisbursementBatchDTOResponse> findRestorableByPayer(@Param("payerId") UUID payerId);

    /** Misma selección que {@link #findRestorableByPayer}, con bloqueo para liquidación total. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM DisbursementBatchModel b
        WHERE b.payer.id = :payerId
          AND b.status = com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum.SETTLED
          AND NOT EXISTS (SELECT h.id FROM CreditFacilityHistoryModel h WHERE h.disbursementBatch = b)
        ORDER BY b.dueDate, b.batchNumber
    """)
    List<DisbursementBatchModel> findRestorableEntitiesByPayerForUpdate(@Param("payerId") UUID payerId);

    /** Valor nominal de los documentos del lote: lo que consumieron del cupo al cargarse. */
    @Query("""
        SELECT COALESCE(SUM(t.document.nominalAmount), 0)
        FROM FinancingTransactionModel t
        WHERE t.disbursementBatch.id = :batchId
    """)
    BigDecimal sumNominalAmount(@Param("batchId") UUID batchId);

}
