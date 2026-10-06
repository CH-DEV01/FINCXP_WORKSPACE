package com.davivienda.factoraje.service.impl;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.davivienda.factoraje.domain.catalogs.RoleCat;
import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.user.UserDTORequest;
import com.davivienda.factoraje.dto.user.UserDTOResponse;
import com.davivienda.factoraje.dto.user.UserUpdateDTORequest;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.AuditText;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.infrastructure.util.IdentifierNormalizer;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.RoleCatRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final EntityRepository entityRepository;
    private final RoleCatRepository roleCatRepository;
    private final CurrentUserService currentUserService;
    private final MailNoticePublisher mailNotices;

    private void validateUniqueness(UserDTORequest entity) {

        if (userRepository.existsByDui(entity.dui())) {
            throw new ResourceAlreadyExistsException("Ya existe un usuario con el DUI " + entity.dui() + ".");
        }

        if (userRepository.existsByEmail(entity.email())) {
            throw new ResourceAlreadyExistsException("Ya existe un usuario con el correo " + entity.email() + ".");
        }
    }

    @Override
    @Transactional
    public UserDTOResponse createUser(UserDTORequest rawRequest) {

        log.info("Starting the creation of a new user");

        UserDTORequest request = new UserDTORequest(
                IdentifierNormalizer.requireDui(rawRequest.dui()),
                rawRequest.entityId(),
                rawRequest.roleId(),
                rawRequest.firstName(),
                rawRequest.lastName(),
                IdentifierNormalizer.requireEmail(rawRequest.email()));

        validateUniqueness(request);

        log.info("Searching for entity with identifier: {}", request.entityId());

        EntityModel entity = entityRepository.findById(request.entityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la entidad con ID: " + request.entityId()));

        RoleCat role = roleCatRepository.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el rol con ID: " + request.roleId()));

        requireAssignableRole(role);
        requireRoleMatchesEntity(role, entity);

        UserModel user = UserModel.builder()
                .dui(request.dui())
                .entity(entity)
                .role(role)
                .status(GeneralStatusEnum.ACTIVE)
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .build();

        try {
            UserModel savedUser = userRepository.saveAndFlush(user);
            log.info("User created successfully with ID: {}, role {} by {}",
                    savedUser.getId(), role.getName(), currentUserService.dui());
            mailNotices.operatorChanged("Usuarios", MailNoticePublisher.NO_RECORD, describe(savedUser));
            return UserDTOResponse.fromEntity(savedUser);

        } catch (DataIntegrityViolationException e) {
            log.error("Integrity violation when creating user", e);
            throw new ResourceAlreadyExistsException("El DUI o el correo ya están registrados para otro usuario.");
        }

    }

    @Override
    @Transactional
    public UserDTOResponse updateUser(UUID id, UserUpdateDTORequest request) {

        log.info("Updating user with ID: {}", id);

        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));

        if (Roles.SYSTEM_ADMIN.equals(user.getRole().getName())) {
            throw new UnauthorizedAccessException(
                    "Los usuarios " + Roles.SYSTEM_ADMIN + " solo se administran directamente en la base de datos.");
        }

        String dui = IdentifierNormalizer.requireDui(request.dui());
        String email = IdentifierNormalizer.requireEmail(request.email());

        if (userRepository.existsByDuiAndIdNot(dui, id)) {
            throw new ResourceAlreadyExistsException("Ya existe otro usuario con el DUI " + dui + ".");
        }
        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new ResourceAlreadyExistsException("Ya existe otro usuario con el correo " + email + ".");
        }

        if (request.status() == GeneralStatusEnum.INACTIVE && currentUserService.is(user)) {
            throw new IllegalArgumentException("No puede inactivar su propio usuario.");
        }

        EntityModel entity = entityRepository.findById(request.entityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Entidad no encontrada con ID: " + request.entityId()));

        RoleCat role = roleCatRepository.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Rol no encontrado con ID: " + request.roleId()));

        RoleCat previousRole = user.getRole();
        boolean roleChanged = !previousRole.getId().equals(role.getId());

        if (roleChanged && currentUserService.is(user)) {
            throw new UnauthorizedAccessException("No puede cambiar su propio rol.");
        }
        requireAssignableRole(role);
        requireRoleMatchesEntity(role, entity);

        String previous = describe(user);
        user.setDui(dui);
        user.setEmail(email);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEntity(entity);
        user.setRole(role);
        user.setStatus(request.status());

        try {
            UserModel saved = userRepository.saveAndFlush(user);
            log.info("User {} updated by {}. Status: {}", saved.getId(), currentUserService.dui(), saved.getStatus());
            if (roleChanged) {
                log.warn("Role change for user {}: {} -> {} by {}",
                        saved.getId(), previousRole.getName(), role.getName(), currentUserService.dui());
            }
            mailNotices.operatorChanged("Usuarios", previous, describe(saved));
            return UserDTOResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            log.error("Integrity violation when updating user {}", id, e);
            throw new ResourceAlreadyExistsException("El DUI o el correo ya están registrados para otro usuario.");
        }
    }

    private void requireAssignableRole(RoleCat role) {
        if (Roles.SYSTEM_ADMIN.equals(role.getName())) {
            throw new UnauthorizedAccessException(
                    "El rol " + Roles.SYSTEM_ADMIN + " solo se asigna directamente en la base de datos.");
        }
    }

    private void requireRoleMatchesEntity(RoleCat role, EntityModel entity) {
        String expectedType = EntityTypeCode.forRole(role.getName());
        if (expectedType == null) {
            throw new IllegalArgumentException("El rol " + role.getName() + " no tiene un tipo de entidad asociado.");
        }
        String actualType = entity.getEntityType() != null ? entity.getEntityType().getCode() : null;
        if (!expectedType.equals(actualType)) {
            throw new IllegalArgumentException("El rol " + role.getName() + " no puede asignarse a la entidad "
                    + entity.getName() + ": requiere una entidad de tipo " + entityTypeLabel(expectedType) + ".");
        }
    }

    private static String describe(UserModel user) {
        return "Usuario " + AuditText.value(user.fullName()) + " (" + AuditText.value(user.getEmail()) + ")"
                + ", DUI " + AuditText.value(user.getDui())
                + ", rol " + (user.getRole() != null ? user.getRole().getName() : "—")
                + ", entidad " + (user.getEntity() != null ? AuditText.value(user.getEntity().getName()) : "—")
                + ", estado " + AuditText.status(user.getStatus());
    }

    private static String entityTypeLabel(String typeCode) {
        return switch (typeCode) {
            case EntityTypeCode.BANK -> "BANCO";
            case EntityTypeCode.PAYER -> "PAGADOR";
            case EntityTypeCode.SUPPLIER -> "PROVEEDOR";
            default -> typeCode;
        };
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDTOResponse> getUsers(String search, Pageable pageable) {
        log.info("Obteniendo usuarios del sistema");
        Page<UserModel> users = StringUtils.hasText(search)
                ? userRepository.search(search.trim(), pageable)
                : userRepository.findAll(pageable);
        return users.map(UserDTOResponse::fromEntity);
    }

}
