package com.davivienda.factoraje.service.impl;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.SystemParameterModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.parameter.ParameterDTORequest;
import com.davivienda.factoraje.dto.parameter.ParameterDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.ParameterRepository;
import com.davivienda.factoraje.service.ParameterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * El operador bancario (ADMIN) ve y edita solo los parámetros de negocio; el
 * administrador del sistema, solo los técnicos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterServiceImpl implements ParameterService {

    private final ParameterRepository parameterRepository;
    private final SystemParameters systemParameters;
    private final CurrentUserService currentUser;

    @Override
    @Transactional(readOnly = true)
    public Page<ParameterDTOResponse> readAllParameters(Pageable pageable) {
        log.info("Reading all parameters");
        Set<String> businessKeys = SystemParameterKey.businessKeys();
        Page<SystemParameterModel> parameters = currentUser.isAdmin()
                ? parameterRepository.findByKeyIn(businessKeys, pageable)
                : parameterRepository.findByKeyNotIn(businessKeys, pageable);
        return parameters.map(ParameterDTOResponse::fromEntity);
    }

    @Override
    @Transactional
    public ParameterDTOResponse updateParameter(UUID id, ParameterDTORequest request) {

        log.info("Starting update for parameter with ID: {}", id);

        SystemParameterModel parameter = parameterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el parámetro con ID: " + id));

        boolean business = SystemParameterKey.isBusiness(parameter.getKey());
        if (business != currentUser.isAdmin()) {
            throw new UnauthorizedAccessException(business
                    ? "El parámetro " + parameter.getKey() + " es de negocio y lo administra el operador bancario."
                    : "El parámetro " + parameter.getKey() + " es técnico y lo administra el administrador del sistema.");
        }

        // El código lee los parámetros por su llave; renombrarla rompería esa lectura.
        if (request.key() != null && !request.key().trim().equals(parameter.getKey())) {
            throw new IllegalArgumentException("La clave de un parámetro no se puede modificar.");
        }

        boolean sensitive = SystemParameterKey.isSensitive(parameter.getKey());
        if (sensitive && request.value() != null && ParameterDTOResponse.MASK.equals(request.value().trim())) {
            throw new IllegalArgumentException("Ingrese el nuevo valor de " + parameter.getKey() + ".");
        }

        String value = SystemParameterKey.find(parameter.getKey())
                .map(key -> key.normalize(request.value()))
                .orElseGet(() -> {
                    if (request.value() == null || request.value().isBlank()) {
                        throw new IllegalArgumentException("El valor del parámetro no puede estar vacío.");
                    }
                    return request.value().trim();
                });

        String previous = parameter.getValue();
        parameter.setValue(value);
        SystemParameterModel parameterUpdated = parameterRepository.saveAndFlush(parameter);
        systemParameters.invalidate();

        if (sensitive) {
            log.info("Parameter {} successfully updated by user {}", parameter.getKey(), currentUser.id());
        } else {
            log.info("Parameter {} successfully updated by user {} from {} to {}",
                    parameter.getKey(), currentUser.id(), previous, value);
        }
        return ParameterDTOResponse.fromEntity(parameterUpdated);

    }

}
