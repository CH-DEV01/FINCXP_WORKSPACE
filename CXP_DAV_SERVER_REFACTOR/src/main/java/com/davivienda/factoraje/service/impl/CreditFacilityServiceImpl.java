package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.CreditFacilityHistoryModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.credit_facility.CreditFacilityDTOResponse;
import com.davivienda.factoraje.dto.credit_facility.RestoreCreditFacilityDTORequest;
import com.davivienda.factoraje.dto.credit_facility_history.CreditFacilityHistoryDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.AuditText;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.repository.CreditFacilityHistoryRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.ProductPricingTermRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.CreditFacilityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreditFacilityServiceImpl implements CreditFacilityService {

    private final CreditFacilityRepository creditFacilityRepository;
    private final CreditFacilityHistoryRepository creditFacilityHistoryRepository;
    private final UserRepository userRepository;
    private final ProductPricingTermRepository productPricingTermRepository;
    private final SystemParameters systemParameters;
    private final MailNoticePublisher mailNotices;

    @Override
    @Transactional(readOnly = true)
    public CreditFacilityDTOResponse findByPayerId(UUID payerId) {

        CreditFacilityModel creditFacility = creditFacilityRepository.findByPayerId(payerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el cupo de crédito para el pagador con ID: " + payerId));

        ProductPricingTermModel pricing = productPricingTermRepository
                .findByCreditFacilityId(creditFacility.getId())
                .orElse(null);

        return CreditFacilityDTOResponse.fromEntity(creditFacility, pricing, defaultThreshold());
    }

    @Override
    @Transactional
    public CreditFacilityDTOResponse restoreCreditFacility(RestoreCreditFacilityDTORequest request,
            String executedBy) {

        if (request.type() != RepaymentTypeEnum.PARTIAL && request.type() != RepaymentTypeEnum.FULL) {
            throw new IllegalArgumentException("El tipo de abono debe ser PARTIAL o FULL.");
        }

        UserModel user = userRepository.findByDui(executedBy)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el usuario especificado."));

        CreditFacilityModel facility = creditFacilityRepository.findByIdForUpdate(request.creditFacilityId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cupo de crédito especificado."));

        BigDecimal currentUsed = facility.getAmountInUse();
        BigDecimal newUsedAmount = currentUsed.subtract(request.amount());

        if (newUsedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(String.format(
                    "El monto a abonar ($%s) supera el consumo actual de la línea ($%s).",
                    request.amount(), currentUsed));
        }

        facility.setAmountInUse(newUsedAmount);
        creditFacilityRepository.save(facility);

        creditFacilityHistoryRepository.save(CreditFacilityHistoryModel.builder()
                .amount(request.amount())
                .creditFacility(facility)
                .executedBy(user)
                .payer(facility.getPayer())
                .referenceNumber(request.reference())
                .repaymentType(request.type())
                .build());

        String payerName = facility.getPayer() != null ? facility.getPayer().getName() : "el pagador";
        mailNotices.operatorChanged("Pagadores",
                "Línea de crédito de " + payerName + ": monto en uso " + AuditText.amount(currentUsed),
                "Línea de crédito de " + payerName + ": monto en uso " + AuditText.amount(newUsedAmount)
                        + " (abono " + AuditText.amount(request.amount()) + ")");
        return CreditFacilityDTOResponse.fromEntity(facility, null, defaultThreshold());
    }

    private BigDecimal defaultThreshold() {
        return systemParameters.getDecimal(SystemParameterKey.DEFAULT_CREDIT_THRESHOLD);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CreditFacilityHistoryDTOResponse> getHistory(UUID creditFacilityId, int page, int size) {
        Pageable pageable = PageRequests.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        return creditFacilityHistoryRepository.findByCreditFacilityId(creditFacilityId, pageable)
                .map(CreditFacilityHistoryDTOResponse::fromEntity);
    }
}
