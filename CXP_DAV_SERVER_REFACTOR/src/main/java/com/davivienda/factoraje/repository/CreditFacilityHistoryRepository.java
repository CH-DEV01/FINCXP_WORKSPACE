package com.davivienda.factoraje.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.CreditFacilityHistoryModel;

@Repository 
public interface CreditFacilityHistoryRepository extends JpaRepository<CreditFacilityHistoryModel, UUID> {

    @EntityGraph(attributePaths = "executedBy")
    Page<CreditFacilityHistoryModel> findByCreditFacilityId(UUID creditFacilityId, Pageable pageable);
    
}
