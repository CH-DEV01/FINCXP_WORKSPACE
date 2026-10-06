package com.davivienda.factoraje.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.davivienda.factoraje.domain.entities.RoleMenuModel;
import com.davivienda.factoraje.dto.auth.MenuDTO;

public interface RoleMenuRepository extends JpaRepository<RoleMenuModel, UUID> {

    /** Menús activos del rol en su orden de despliegue. */
    @Query("""
        SELECT new com.davivienda.factoraje.dto.auth.MenuDTO(m.label, m.path, m.icon, m.description)
        FROM RoleMenuModel rm
        JOIN rm.menu m
        WHERE rm.role.id = :roleId
        AND m.status = com.davivienda.factoraje.domain.enums.GeneralStatusEnum.ACTIVE
        ORDER BY rm.displayOrder ASC, m.label ASC
    """)
    List<MenuDTO> findActiveMenusByRole(@Param("roleId") UUID roleId);
}
