package com.davivienda.factoraje.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.UploadBatchModel;

@Repository 
public interface UploadBatchRepository extends JpaRepository<UploadBatchModel, UUID>{

    boolean existsByBatchNumber(String batchNumber);
    
}
