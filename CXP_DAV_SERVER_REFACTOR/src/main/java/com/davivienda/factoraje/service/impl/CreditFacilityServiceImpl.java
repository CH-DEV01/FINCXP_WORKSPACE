package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.CreditFacilityHistoryModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;
import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.credit_facility.CreditFacilityDTOResponse;
import com.davivienda.factoraje.dto.credit_facility.RestorableDisbursementBatchDTOResponse;
import com.davivienda.factoraje.dto.credit_facility.RestoreCreditFacilityDTORequest;
import com.davivienda.factoraje.dto.credit_facility_history.CreditFacilityHistoryDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.AuditText;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.repository.CreditFacilityHistoryRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.DisbursementBatchRepository;
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
    private final DisbursementBatchRepository disbursementBatchRepository;
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

        DisbursementBatchModel batch = null;
        BigDecimal amount;
        if (request.type() == RepaymentTypeEnum.PARTIAL) {
            batch = repayableBatch(facility, request.disbursementBatchId());
            amount = disbursementBatchRepository.sumNominalAmount(batch.getId());
            if (amount == null || amount.signum() <= 0) {
                throw new IllegalArgumentException(
                        "El lote " + batch.getBatchNumber() + " no tiene documentos que abonar al cupo.");
            }
        } else {
            if (request.amount() == null) {
                throw new IllegalArgumentException("El monto a abonar es obligatorio.");
            }
            amount = request.amount();
        }

        BigDecimal currentUsed = facility.getAmountInUse();
        BigDecimal newUsedAmount = currentUsed.subtract(amount);

        if (newUsedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(String.format(
                    "El monto a abonar ($%s) supera el consumo actual del cupo ($%s).",
                    Money.roundOrZero(amount), Money.roundOrZero(currentUsed)));
        }

        facility.setAmountInUse(newUsedAmount);
        creditFacilityRepository.save(facility);

        creditFacilityHistoryRepository.save(CreditFacilityHistoryModel.builder()
                .amount(amount)
                .creditFacility(facility)
                .executedBy(user)
                .payer(facility.getPayer())
                .referenceNumber(request.reference())
                .repaymentType(request.type())
                .disbursementBatch(batch)
                .build());

        String payerName = facility.getPayer() != null ? facility.getPayer().getName() : "el pagador";
        String batchText = batch != null ? ", lote " + batch.getBatchNumber() : "";
        mailNotices.operatorChanged("Pagadores",
                "Cupo de crédito de " + payerName + ": monto en uso " + AuditText.amount(currentUsed),
                "Cupo de crédito de " + payerName + ": monto en uso " + AuditText.amount(newUsedAmount)
                        + " (abono " + AuditText.amount(amount) + batchText + ")");
        return CreditFacilityDTOResponse.fromEntity(facility, null, defaultThreshold());
    }

    /** El pago parcial liquida un lote desembolsado del mismo pagador que aún no se ha abonado. */
    private DisbursementBatchModel repayableBatch(CreditFacilityModel facility, UUID batchId) {
        if (batchId == null) {
            throw new IllegalArgumentException("Seleccione el lote de desembolso que liquida el pago parcial.");
        }

        DisbursementBatchModel batch = disbursementBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el lote de desembolso especificado."));

        if (batch.getPayer() == null || facility.getPayer() == null
                || !batch.getPayer().getId().equals(facility.getPayer().getId())) {
            throw new IllegalArgumentException(
                    "El lote " + batch.getBatchNumber() + " no pertenece al pagador de este cupo.");
        }
        if (batch.getStatus() != DisbursementBatchStatusEnum.SETTLED) {
            throw new IllegalArgumentException(
                    "El lote " + batch.getBatchNumber() + " aún no está desembolsado; no se puede abonar al cupo.");
        }
        if (creditFacilityHistoryRepository.existsByDisbursementBatchId(batchId)) {
            throw new ResourceAlreadyExistsException(
                    "El lote " + batch.getBatchNumber() + " ya fue abonado al cupo.");
        }
        return batch;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestorableDisbursementBatchDTOResponse> getRestorableBatches(UUID creditFacilityId) {

        CreditFacilityModel facility = creditFacilityRepository.findById(creditFacilityId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el cupo de crédito especificado."));

        if (facility.getPayer() == null) {
            return List.of();
        }
        return disbursementBatchRepository.findRestorableByPayer(facility.getPayer().getId());
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
