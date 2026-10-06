package com.davivienda.factoraje.service.impl;

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
import com.davivienda.factoraje.repository.ParameterRepository;
import com.davivienda.factoraje.service.ParameterService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParameterServiceImpl implements ParameterService {

    private final ParameterRepository parameterRepository;
    private final SystemParameters systemParameters;


    @Override
    @Transactional(readOnly = true)
    public Page<ParameterDTOResponse> readAllParameters(Pageable pageable) {
        log.info("Reading all parameters");
        return parameterRepository.findAll(pageable)
                .map(ParameterDTOResponse::fromEntity);
    }

    @Override
    @Transactional
    public ParameterDTOResponse updateParameter(UUID id, ParameterDTORequest request) {

        log.info("Starting update for parameter with ID: {}", id);

        SystemParameterModel parameter = parameterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el parámetro con ID: " + id));

        // El código lee los parámetros por su llave; renombrarla rompería esa lectura.
        if (request.key() != null && !request.key().trim().equals(parameter.getKey())) {
            throw new IllegalArgumentException("La clave de un parámetro no se puede modificar.");
        }

        String value = SystemParameterKey.find(parameter.getKey())
                .map(key -> key.normalize(request.value()))
                .orElseGet(() -> {
                    if (request.value() == null || request.value().isBlank()) {
                        throw new IllegalArgumentException("El valor del parámetro no puede estar vacío.");
                    }
                    return request.value().trim();
                });

        parameter.setValue(value);
        SystemParameterModel parameterUpdated = parameterRepository.saveAndFlush(parameter);
        systemParameters.invalidate();

        if (SystemParameterKey.find(parameter.getKey()).map(SystemParameterKey::sensitive).orElse(false)) {
            log.info("Parameter {} successfully updated", parameter.getKey());
        } else {
            log.info("Parameter {} successfully updated to {}", parameter.getKey(), value);
        }
        return ParameterDTOResponse.fromEntity(parameterUpdated);

    }

}
