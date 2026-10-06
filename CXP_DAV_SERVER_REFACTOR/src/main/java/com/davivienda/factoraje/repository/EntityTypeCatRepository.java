package com.davivienda.factoraje.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.catalogs.EntityTypeCat;

@Repository 
public interface EntityTypeCatRepository extends JpaRepository<EntityTypeCat, UUID>{

    Optional<EntityTypeCat> findByCode(String code);
    
}
