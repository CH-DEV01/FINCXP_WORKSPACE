package com.davivienda.factoraje.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.davivienda.factoraje.domain.catalogs.RoleCat;

public interface RoleCatRepository extends JpaRepository<RoleCat, UUID> {

    Optional<RoleCat> findByName(String name);
    
}
