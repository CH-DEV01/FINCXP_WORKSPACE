package com.davivienda.factoraje.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.dto.role.RoleDTOResponse;
import com.davivienda.factoraje.repository.RoleCatRepository;
import com.davivienda.factoraje.service.RoleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleCatRepository roleCatRepository;

    /** Roles que se pueden asignar desde Gestión de usuarios; SYSTEM_ADMIN solo se asigna en la base de datos. */
    @Override
    @Transactional(readOnly = true)
    public List<RoleDTOResponse> getRoles() {
        return roleCatRepository.findAll().stream()
                .filter(role -> !Roles.SYSTEM_ADMIN.equals(role.getName()))
                .map(RoleDTOResponse::fromEntity)
                .toList();
    }

}
