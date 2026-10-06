package com.davivienda.factoraje.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.SystemParameterModel;

@Repository 
public interface ParameterRepository extends JpaRepository<SystemParameterModel, UUID>{

    Optional <SystemParameterModel> findByKey(String key);

    Page<SystemParameterModel> findAll(Pageable pageable);

}
