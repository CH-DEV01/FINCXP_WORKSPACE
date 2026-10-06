package com.davivienda.factoraje.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.role.RoleDTOResponse;
import com.davivienda.factoraje.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;


    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDTOResponse>>> getRoles() {

        return ResponseEntity.ok(ApiResponse.success(roleService.getRoles(), "Catálogo de roles obtenido exitosamente."));
    }

}
