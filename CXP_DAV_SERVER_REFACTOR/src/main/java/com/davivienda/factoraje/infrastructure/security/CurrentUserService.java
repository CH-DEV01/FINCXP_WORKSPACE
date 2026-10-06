package com.davivienda.factoraje.infrastructure.security;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Usuario autenticado y validaciones de propiedad. Los roles por endpoint se
 * definen en {@code SecurityConfig}; aquí se valida que un proveedor o pagador
 * solo opere sobre datos de su propia entidad. ADMIN opera sobre cualquier
 * entidad.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final MasterAgreementRepository masterAgreementRepository;
    private final UserRepository userRepository;

    /**
     * Usuario del token, con su rol y entidad ya cargados. Es una instancia
     * desconectada de la transacción actual; para asignarlo como relación de
     * otra entidad usar {@link #managed()}.
     */
    public UserModel get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserModel user)) {
            throw new UnauthorizedAccessException("No hay un usuario autenticado.");
        }
        return user;
    }

    /** Usuario autenticado leído en la transacción actual. */
    public UserModel managed() {
        return userRepository.findById(id())
                .orElseThrow(() -> new UnauthorizedAccessException("El usuario autenticado ya no existe."));
    }

    public UUID id() {
        return get().getId();
    }

    public String dui() {
        return get().getDui();
    }

    public UUID entityId() {
        return get().getEntity().getId();
    }

    public boolean isAdmin() {
        return Roles.ADMIN.equals(get().getRole().getName());
    }

    public boolean is(UserModel user) {
        return Objects.equals(id(), user.getId());
    }

    public void requireOwnEntity(UUID entityId) {
        if (isAdmin()) {
            return;
        }
        if (!Objects.equals(entityId(), entityId)) {
            throw new UnauthorizedAccessException("No tiene acceso a la información de otra entidad.");
        }
    }

    public void requireOwnMasterAgreement(UUID masterAgreementId) {
        if (isAdmin()) {
            return;
        }
        if (!masterAgreementRepository.isEntityPartOfAgreement(masterAgreementId, entityId())) {
            throw new UnauthorizedAccessException("No tiene acceso a este convenio.");
        }
    }
}
