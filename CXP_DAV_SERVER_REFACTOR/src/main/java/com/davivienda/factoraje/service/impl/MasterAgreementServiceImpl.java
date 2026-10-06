package com.davivienda.factoraje.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.disbursement_policy.NextDisbursementDateDTOResponse;
import com.davivienda.factoraje.dto.master_agreement.MasterAgreementDTORequest;
import com.davivienda.factoraje.dto.master_agreement.MasterAgreementDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.AuditText;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.repository.PaymentPolicyCatRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.MasterAgreementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MasterAgreementServiceImpl implements MasterAgreementService {

    private final MasterAgreementRepository masterAgreementRepository;
    private final DisbursementPolicyCatRepository disbursementPolicyCatRepository;
    private final PaymentPolicyCatRepository paymentPolicyCatRepository;
    private final EntityRepository entityRepository;
    private final DisbursementPolicyService disbursementPolicyService;
    private final MailNoticePublisher mailNotices;

    private static String describe(MasterAgreementModel agreement) {
        String payer = agreement.getPayer() != null ? agreement.getPayer().getName() : null;
        String supplier = agreement.getSupplier() != null ? agreement.getSupplier().getName() : null;
        String paymentPolicy = agreement.getPaymentPolicy() != null ? agreement.getPaymentPolicy().getCode() : null;
        String disbursementPolicy = agreement.getDisbursementPolicy() != null
                ? agreement.getDisbursementPolicy().getName() : null;
        return "Convenio " + AuditText.value(payer) + " – " + AuditText.value(supplier)
                + ", política de pago " + AuditText.value(paymentPolicy)
                + ", política de desembolso " + AuditText.value(disbursementPolicy);
    }

    private void validateUniqueness(MasterAgreementDTORequest request) {
        log.debug("Validando unicidad del convenio: pagador {}, proveedor {}, tipo {}",
                request.payerId(), request.supplierId(), request.agreementType().name());

        boolean exists = masterAgreementRepository.existsByPayerIdAndSupplierIdAndAgreementType(
                request.payerId(),
                request.supplierId(),
                request.agreementType());

        if (exists) {
            log.warn("Intento de duplicar el convenio marco entre el pagador {} y el proveedor {}.",
                    request.payerId(), request.supplierId());
            throw new ResourceAlreadyExistsException("El convenio marco ya existe.");
        }
    }

    @Override
    @Transactional
    public MasterAgreementDTOResponse createMasterAgreement(MasterAgreementDTORequest request) {

        log.info("Creando convenio marco");

        requirePresent(request.agreementType(), "El tipo de convenio es obligatorio.");
        requirePresent(request.payerId(), "El ID del pagador es obligatorio.");
        requirePresent(request.supplierId(), "El ID del proveedor es obligatorio.");

        validateUniqueness(request);

        PaymentPolicyCat paymentPolicy = paymentPolicyCatRepository.findById(request.paymentPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la política de pago con ID: " + request.paymentPolicyId()));

        DisbursementPolicyCat disbursementPolicy = disbursementPolicyCatRepository
                .findById(request.disbursementPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la política de desembolso con ID: " + request.disbursementPolicyId()));

        EntityModel supplier = entityRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el proveedor con ID: " + request.supplierId()));

        EntityModel payer = entityRepository.findById(request.payerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el pagador con ID: " + request.payerId()));

        MasterAgreementModel masterAgreementModel = MasterAgreementModel.builder()
                .agreementType(request.agreementType())
                .disbursementPolicy(disbursementPolicy)
                .paymentPolicy(paymentPolicy)
                .supplier(supplier)
                .payer(payer)
                .status(GeneralStatusEnum.ACTIVE)
                .build();

        MasterAgreementModel savedMasterAgreement = masterAgreementRepository.save(masterAgreementModel);
        log.info("Convenio marco creado con ID: {}", savedMasterAgreement.getId());
        mailNotices.operatorChanged("Acuerdos", MailNoticePublisher.NO_RECORD, describe(masterAgreementModel));
        return MasterAgreementDTOResponse.fromEntity(savedMasterAgreement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MasterAgreementDTOResponse> getPaginatedAgreements(int page, int size, String search, UUID payerId) {
        Pageable pageable = PageRequests.of(page, size, Sort.by("createdAt").descending());
        boolean hasSearch = StringUtils.hasText(search);
        Page<MasterAgreementModel> agreements;
        if (payerId != null) {
            agreements = hasSearch
                    ? masterAgreementRepository.searchByPayer(payerId, search.trim(), pageable)
                    : masterAgreementRepository.findByPayerId(payerId, pageable);
        } else {
            agreements = hasSearch
                    ? masterAgreementRepository.search(search.trim(), pageable)
                    : masterAgreementRepository.findAll(pageable);
        }
        return agreements.map(MasterAgreementDTOResponse::fromEntity);
    }

    @Override
    @Transactional
    public MasterAgreementDTOResponse updateMasterAgreement(UUID masterAgreementId, MasterAgreementDTORequest request) {

        log.info("Actualizando el convenio marco {}", masterAgreementId);

        MasterAgreementModel existingAgreement = masterAgreementRepository.findById(masterAgreementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el convenio marco con ID: " + masterAgreementId));

        PaymentPolicyCat paymentPolicy = paymentPolicyCatRepository.findById(request.paymentPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la política de pago con ID: " + request.paymentPolicyId()));

        DisbursementPolicyCat disbursementPolicy = disbursementPolicyCatRepository
                .findById(request.disbursementPolicyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la política de desembolso con ID: " + request.disbursementPolicyId()));

        String previous = describe(existingAgreement);
        // Solo las políticas son editables; estado, tipo, pagador y proveedor se ignoran aunque vengan en el request.
        existingAgreement.setPaymentPolicy(paymentPolicy);
        existingAgreement.setDisbursementPolicy(disbursementPolicy);

        MasterAgreementModel updatedAgreement = masterAgreementRepository.save(existingAgreement);
        log.info("Convenio marco {} actualizado", updatedAgreement.getId());
        mailNotices.operatorChanged("Acuerdos", previous, describe(existingAgreement));

        return MasterAgreementDTOResponse.fromEntity(updatedAgreement);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MasterAgreementDTOResponse> getMasterAgreementsBySupplier(UUID supplierId, boolean onlyActive) {

        log.info("Consultando convenios marco del proveedor {}", supplierId);

        if (!entityRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException("No se encontró el proveedor con ID: " + supplierId);
        }

        return masterAgreementRepository.findBySupplierId(supplierId).stream()
                .filter(agreement -> !onlyActive || isUsable(agreement))
                .map(MasterAgreementDTOResponse::fromEntity)
                .toList();
    }

    private static void requirePresent(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /** La línea y el tarifario se activan e inactivan junto con el pagador. */
    private static boolean isUsable(MasterAgreementModel agreement) {
        return agreement.getStatus() == GeneralStatusEnum.ACTIVE
                && agreement.getPayer().getStatus() == GeneralStatusEnum.ACTIVE;
    }

    @Override
    @Transactional(readOnly = true)
    public NextDisbursementDateDTOResponse getNextDisbursementDate(UUID masterAgreementId) {

        log.info("Calculando la próxima fecha de desembolso del convenio marco {}", masterAgreementId);

        MasterAgreementModel masterAgreement = masterAgreementRepository.findById(masterAgreementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el convenio marco con ID: " + masterAgreementId));

        DisbursementPolicyCat disbursementPolicy = masterAgreement.getDisbursementPolicy();
        if (disbursementPolicy == null) {
            throw new ResourceNotFoundException(
                    "El convenio marco no tiene política de desembolso configurada: " + masterAgreementId);
        }

        LocalDate nextDate = disbursementPolicyService.calculateDisbursementDateForNow(disbursementPolicy);

        return new NextDisbursementDateDTOResponse(
                nextDate,
                disbursementPolicy.getName(),
                disbursementPolicy.getCode(),
                masterAgreementId,
                disbursementPolicyService.cutoffTime());
    }
}
