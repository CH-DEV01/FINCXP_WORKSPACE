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

import com.davivienda.factoraje.domain.entities.DispersionBatchModel;

import jakarta.persistence.LockModeType;

@Repository
public interface DispersionBatchRepository extends JpaRepository<DispersionBatchModel, UUID> {

    boolean existsByBatchNumber(String batchNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM DispersionBatchModel b WHERE b.id = :id")
    Optional<DispersionBatchModel> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
        SELECT b FROM DispersionBatchModel b
        JOIN FETCH b.payer
        JOIN FETCH b.createdBy
        LEFT JOIN FETCH b.confirmedBy
        LEFT JOIN FETCH b.signer
        WHERE b.id = :id
    """)
    Optional<DispersionBatchModel> findDetailById(@Param("id") UUID id);

    @Query(value = """
        SELECT b FROM DispersionBatchModel b
        JOIN FETCH b.payer
        JOIN FETCH b.createdBy
        LEFT JOIN FETCH b.confirmedBy
    """, countQuery = "SELECT COUNT(b) FROM DispersionBatchModel b")
    Page<DispersionBatchModel> findPageWithUsers(Pageable pageable);

    @Query(value = """
        SELECT b FROM DispersionBatchModel b
        JOIN FETCH b.payer p
        JOIN FETCH b.createdBy
        LEFT JOIN FETCH b.confirmedBy
        WHERE p.id = :payerId
    """, countQuery = "SELECT COUNT(b) FROM DispersionBatchModel b WHERE b.payer.id = :payerId")
    Page<DispersionBatchModel> findPageByPayerWithUsers(@Param("payerId") UUID payerId, Pageable pageable);

    @Query("""
        SELECT b FROM DispersionBatchModel b
        JOIN FETCH b.payer p
        JOIN FETCH b.createdBy
        LEFT JOIN FETCH b.confirmedBy
        WHERE p.id = :payerId
        ORDER BY b.createdAt DESC
    """)
    List<DispersionBatchModel> findAllByPayerWithUsersOrderByCreatedAtDesc(@Param("payerId") UUID payerId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM DispersionBatchModel b")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM DispersionBatchModel b WHERE b.payer.id = :payerId")
    BigDecimal sumTotalAmountByPayer(@Param("payerId") UUID payerId);
}
