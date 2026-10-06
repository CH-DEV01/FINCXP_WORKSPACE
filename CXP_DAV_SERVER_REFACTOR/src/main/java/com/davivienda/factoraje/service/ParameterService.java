package com.davivienda.factoraje.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.davivienda.factoraje.dto.parameter.ParameterDTORequest;
import com.davivienda.factoraje.dto.parameter.ParameterDTOResponse;

public interface ParameterService {

    Page<ParameterDTOResponse> readAllParameters(Pageable pageable);

    ParameterDTOResponse updateParameter(UUID id, ParameterDTORequest request);
    
}
