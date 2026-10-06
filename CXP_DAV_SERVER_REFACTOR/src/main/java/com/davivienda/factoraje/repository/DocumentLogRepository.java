package com.davivienda.factoraje.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.DocumentLogModel;

@Repository 
public interface DocumentLogRepository extends JpaRepository<DocumentLogModel, UUID> {
    
}
