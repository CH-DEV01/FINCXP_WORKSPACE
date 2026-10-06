package com.davivienda.factoraje.service.impl;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.product_pricing_term.ProductPricingTermDTORequest;
import com.davivienda.factoraje.dto.product_pricing_term.ProductPricingTermDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.AuditText;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.ProductPricingTermRepository;
import com.davivienda.factoraje.service.ProductPricingTermService;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductPricingTermServiceImpl implements ProductPricingTermService {

    private final ProductPricingTermRepository productPricingTermRepository;
    private final CreditFacilityRepository creditFacilityRepository;
    private final MailNoticePublisher mailNotices;


    private void validateUniqueness(ProductPricingTermDTORequest entity) {
        if (productPricingTermRepository.existsByCreditFacilityId(entity.creditFacilityId())) {
            log.warn("La línea de crédito indicada ya tiene un tarifario registrado.");
            throw new ResourceAlreadyExistsException(
                    "La línea de crédito indicada ya tiene un tarifario registrado.");
        }
    }

    @Transactional
    @Override
    public ProductPricingTermDTOResponse createProductPricingTerm(ProductPricingTermDTORequest request) {

        log.info("Starting the creation of a new product pricing term");
        validateUniqueness(request);

        CreditFacilityModel creditFacility = creditFacilityRepository.findById(request.creditFacilityId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la línea de crédito con ID: " + request.creditFacilityId()));

        ProductPricingTermModel productPricingTerm = ProductPricingTermModel.builder()
                .creditFacility(creditFacility)
                .calculationBase(request.calculationBase())
                .interestRate(request.interestRate())
                .commissionRate(request.commissionRate())
                .status(GeneralStatusEnum.ACTIVE)
                .build();

        ProductPricingTermModel savedProductPricingTerm = productPricingTermRepository.save(productPricingTerm);
        log.info("Product pricing term created successfully with ID: {}", savedProductPricingTerm.getId());
        String payerName = creditFacility.getPayer() != null ? creditFacility.getPayer().getName() : "la línea de crédito";
        mailNotices.operatorChanged("Acuerdos", MailNoticePublisher.NO_RECORD,
                "Condición de precio de " + payerName
                        + ": tasa de interés " + AuditText.value(productPricingTerm.getInterestRate())
                        + ", comisión " + AuditText.value(productPricingTerm.getCommissionRate())
                        + ", base de cálculo " + AuditText.value(productPricingTerm.getCalculationBase()));
        return ProductPricingTermDTOResponse.fromEntity(savedProductPricingTerm);

    }

}
