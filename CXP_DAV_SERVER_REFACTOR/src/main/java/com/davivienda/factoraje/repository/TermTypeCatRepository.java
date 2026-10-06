package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.catalogs.TermTypeCat;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

@Repository
public interface TermTypeCatRepository extends JpaRepository<TermTypeCat, UUID> {

    Optional<TermTypeCat> findByUniqueCode(String uniqueCode);

    List<TermTypeCat> findByStatusOrderByTermNameAsc(GeneralStatusEnum status);

}
