package com.davivienda.factoraje.repository;

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

}
