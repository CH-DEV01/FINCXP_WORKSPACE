package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.enums.AgreementTypeEnum;

@Repository
public interface MasterAgreementRepository extends JpaRepository<MasterAgreementModel, UUID> {

    boolean existsByPayerIdAndSupplierIdAndAgreementType(
            UUID payerId,
            UUID supplierId,
            AgreementTypeEnum agreementType);

    @Override
    @EntityGraph(attributePaths = {"payer", "supplier", "paymentPolicy", "disbursementPolicy"})
    Page<MasterAgreementModel> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"payer", "supplier", "paymentPolicy", "disbursementPolicy"})
    @Query(value = "SELECT ma FROM MasterAgreementModel ma " + SEARCH_FILTER,
            countQuery = "SELECT COUNT(ma) FROM MasterAgreementModel ma " + SEARCH_FILTER)
    Page<MasterAgreementModel> search(@Param("search") String search, Pageable pageable);

    String SEARCH_FILTER = "WHERE LOWER(ma.payer.name) LIKE LOWER(CONCAT('%', :search, '%')) "
            + "OR LOWER(ma.supplier.name) LIKE LOWER(CONCAT('%', :search, '%'))";

    @EntityGraph(attributePaths = {"payer", "supplier", "paymentPolicy", "disbursementPolicy"})
    Page<MasterAgreementModel> findByPayerId(UUID payerId, Pageable pageable);

    @EntityGraph(attributePaths = {"payer", "supplier", "paymentPolicy", "disbursementPolicy"})
    @Query(value = "SELECT ma FROM MasterAgreementModel ma " + PAYER_SEARCH_FILTER,
            countQuery = "SELECT COUNT(ma) FROM MasterAgreementModel ma " + PAYER_SEARCH_FILTER)
    Page<MasterAgreementModel> searchByPayer(@Param("payerId") UUID payerId, @Param("search") String search,
            Pageable pageable);

    String PAYER_SEARCH_FILTER = "WHERE ma.payer.id = :payerId "
            + "AND LOWER(ma.supplier.name) LIKE LOWER(CONCAT('%', :search, '%'))";

    @EntityGraph(attributePaths = {"payer", "supplier", "paymentPolicy", "disbursementPolicy"})
    List<MasterAgreementModel> findBySupplierId(UUID supplierId);

    Optional<MasterAgreementModel> findByPayerIdAndSupplierId(UUID payerId, UUID supplierId);

    @Query("SELECT COUNT(ma) > 0 FROM MasterAgreementModel ma " +
            "WHERE ma.id = :agreementId " +
            "AND (ma.payer.id = :entityId OR ma.supplier.id = :entityId)")
    boolean isEntityPartOfAgreement(@Param("agreementId") UUID agreementId, @Param("entityId") UUID entityId);
}
