package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.enums.TermVersionStatusEnum;

@Repository
public interface TermVersionCatRepository extends JpaRepository<TermVersionCat, UUID> {

    Optional<TermVersionCat> findFirstByStatusAndTermType_UniqueCodeOrderByCreatedAtDesc(
            TermVersionStatusEnum status,
            String uniqueCode);

    @EntityGraph(attributePaths = { "termType", "publishedBy" })
    List<TermVersionCat> findByTermType_UniqueCodeOrderByCreatedAtDesc(String uniqueCode);

    List<TermVersionCat> findByTermType_IdAndStatus(UUID termTypeId, TermVersionStatusEnum status);

    boolean existsByTermType_IdAndVersionNumberIgnoreCase(UUID termTypeId, String versionNumber);

    boolean existsByTermType_IdAndVersionNumberIgnoreCaseAndIdNot(UUID termTypeId, String versionNumber, UUID id);

}
