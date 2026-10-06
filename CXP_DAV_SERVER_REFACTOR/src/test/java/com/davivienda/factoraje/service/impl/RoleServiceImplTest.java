package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.catalogs.RoleCat;
import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.role.RoleDTOResponse;
import com.davivienda.factoraje.repository.RoleCatRepository;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleCatRepository roleCatRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void onlyRolesAssignableFromUserManagementAreListed() {
        when(roleCatRepository.findAll()).thenReturn(List.of(
                role(Roles.ADMIN), role(Roles.SYSTEM_ADMIN), role(Roles.PAYER), role(Roles.SUPPLIER)));

        assertThat(roleService.getRoles())
                .extracting(RoleDTOResponse::name)
                .containsExactly(Roles.ADMIN, Roles.PAYER, Roles.SUPPLIER);
    }

    private static RoleCat role(String name) {
        return RoleCat.builder()
                .id(UUID.randomUUID())
                .name(name)
                .description(name)
                .status(GeneralStatusEnum.ACTIVE)
                .build();
    }
}
