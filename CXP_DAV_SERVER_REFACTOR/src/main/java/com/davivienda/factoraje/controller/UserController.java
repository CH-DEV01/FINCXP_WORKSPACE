package com.davivienda.factoraje.controller;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.PageResponse;
import com.davivienda.factoraje.dto.user.UserDTORequest;
import com.davivienda.factoraje.dto.user.UserDTOResponse;
import com.davivienda.factoraje.dto.user.UserUpdateDTORequest;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final Set<String> SORT_FIELDS = Set.of(
            "firstName", "lastName", "email", "dui", "status", "createdAt");

    private final UserService userService;


    @PostMapping
    public ResponseEntity<ApiResponse<UserDTOResponse>> create(@Valid @RequestBody UserDTORequest request) {

        log.info("Initiating the creation of a new user");

        UserDTOResponse response = userService.createUser(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(ApiResponse.success(response, "Usuario creado exitosamente."));

    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTOResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateDTORequest request) {

        log.info("Updating user {}", id);

        UserDTOResponse response = userService.updateUser(id, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Usuario actualizado correctamente."));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserDTOResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir,
            @RequestParam(required = false) String search) {

        Pageable pageable = createPageRequest(page, size, sortBy, sortDir);
        PageResponse<UserDTOResponse> users = PageResponse.from(userService.getUsers(search, pageable));

        return ResponseEntity.ok(ApiResponse.success(users, "Usuarios obtenidos exitosamente."));
    }

    private Pageable createPageRequest(int page, int size, String sortBy, String sortDir) {
        return PageRequests.of(page, size, sortBy, sortDir, SORT_FIELDS);
    }

}
