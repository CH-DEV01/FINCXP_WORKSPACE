package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.ExcelTemplateColumnModel;

@Repository 
public interface ExcelTemplateColumnRepository extends JpaRepository<ExcelTemplateColumnModel, UUID> {

    List<ExcelTemplateColumnModel> findByActiveTrue();
}
