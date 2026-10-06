package com.davivienda.factoraje.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;

public interface DisbursementPolicyCatRepository extends JpaRepository<DisbursementPolicyCat, UUID>{
 
    Optional<DisbursementPolicyCat> findByCode(String code);
}
