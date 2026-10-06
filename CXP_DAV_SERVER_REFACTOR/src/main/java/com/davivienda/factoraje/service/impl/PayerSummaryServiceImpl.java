package com.davivienda.factoraje.service.impl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.payer.PayerSummaryDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.service.PayerSummaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PayerSummaryServiceImpl implements PayerSummaryService {

    private final CurrentUserService currentUser;
    private final EntityRepository entityRepository;
    private final CreditFacilityRepository creditFacilityRepository;
    private final SystemParameters systemParameters;

    @Override
    @Transactional(readOnly = true)
    public PayerSummaryDTOResponse getOwnSummary() {
        if (currentUser.get().getEntity() == null) {
            throw new UnauthorizedAccessException("El usuario no está asociado a un pagador.");
        }
        return getSummary(currentUser.entityId());
    }

    @Override
    @Transactional(readOnly = true)
    public PayerSummaryDTOResponse getSummary(UUID payerId) {
        EntityModel payer = entityRepository.findById(payerId)
                .filter(entity -> EntityTypeCode.PAYER.equals(entity.getEntityType().getCode()))
                .orElseThrow(() -> new ResourceNotFoundException("El pagador especificado no existe."));

        return PayerSummaryDTOResponse.of(payer,
                creditFacilityRepository.findByPayerId(payer.getId()).orElse(null),
                systemParameters.getDecimal(SystemParameterKey.DEFAULT_CREDIT_THRESHOLD));
    }
}
