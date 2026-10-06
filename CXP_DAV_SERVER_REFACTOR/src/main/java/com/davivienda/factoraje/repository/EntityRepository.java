package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.EntityModel;

import jakarta.persistence.LockModeType;

@Repository
public interface EntityRepository extends JpaRepository<EntityModel, UUID> {

    boolean existsByNit(String nit);

    boolean existsByName(String name);

    @Query("SELECT e FROM EntityModel e WHERE e.entityType.code = :typeCode")
    Page<EntityModel> findAllByEntityTypeCode(
            @Param("typeCode") String typeCode, 
            Pageable pageable
    );

    /** {@code pattern} ya en minúsculas, con comodines y escapado con '!'. */
    @Query("""
        SELECT e FROM EntityModel e
        WHERE e.entityType.code = :typeCode
        AND (LOWER(e.name) LIKE :pattern ESCAPE '!'
            OR LOWER(e.nit) LIKE :pattern ESCAPE '!')
    """)
    Page<EntityModel> searchByEntityTypeCode(
            @Param("typeCode") String typeCode,
            @Param("pattern") String pattern,
            Pageable pageable);

    @Query("SELECT e FROM EntityModel e JOIN FETCH e.entityType WHERE e.nit = :nit")
    Optional<EntityModel> findByNit(@Param("nit") String nit);

    @Query("SELECT e FROM EntityModel e WHERE e.entityType.code = :typeCode ORDER BY e.name ASC")
    List<EntityModel> findAllByEntityTypeCodeOrderByName(@Param("typeCode") String typeCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EntityModel e WHERE e.id = :id")
    Optional<EntityModel> findByIdForUpdate(@Param("id") UUID id);

}
