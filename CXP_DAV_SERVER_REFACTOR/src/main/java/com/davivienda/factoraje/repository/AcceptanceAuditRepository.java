package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.AcceptanceAuditModel;

@Repository 
public interface AcceptanceAuditRepository extends JpaRepository<AcceptanceAuditModel, UUID> {

    /** Filas [versionId (UUID), cantidad (Long)] de aceptaciones por versión del tipo indicado. */
    @Query("""
            SELECT a.termVersion.id, COUNT(a)
            FROM AcceptanceAuditModel a
            WHERE a.termVersion.termType.uniqueCode = :uniqueCode
            GROUP BY a.termVersion.id
            """)
    List<Object[]> countByVersionForTermType(@Param("uniqueCode") String uniqueCode);

    long countByTermVersionId(UUID termVersionId);

}
