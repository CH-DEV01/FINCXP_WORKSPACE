package com.davivienda.factoraje.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.FinancingRequestModel;

@Repository 
public interface FinancingRequestRepository extends JpaRepository<FinancingRequestModel, UUID> {
}
