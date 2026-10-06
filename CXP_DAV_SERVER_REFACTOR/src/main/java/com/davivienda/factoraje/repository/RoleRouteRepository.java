package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.davivienda.factoraje.domain.entities.RoleRouteModel;
import com.davivienda.factoraje.dto.auth.RouteDTO;

public interface RoleRouteRepository extends JpaRepository<RoleRouteModel, UUID> {

    /** Rutas activas del rol, con la pantalla inicial primero. */
    @Query("""
        SELECT new com.davivienda.factoraje.dto.auth.RouteDTO(r.path, r.componentName, rr.isIndex)
        FROM RoleRouteModel rr
        JOIN rr.route r
        WHERE rr.role.id = :roleId
        AND r.status = com.davivienda.factoraje.domain.enums.GeneralStatusEnum.ACTIVE
        ORDER BY rr.isIndex DESC, r.path ASC
    """)
    List<RouteDTO> findActiveRoutesByRole(@Param("roleId") UUID roleId);
}
