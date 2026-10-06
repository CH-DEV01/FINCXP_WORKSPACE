package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.CreditFacilityHistoryModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DocumentLogModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.QuarantineReasonEnum;
import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;
import com.davivienda.factoraje.dto.document.DocumentDTOResponse;
import com.davivienda.factoraje.dto.document.DocumentHistoryDTOResponse;
import com.davivienda.factoraje.dto.document.DocumentHistorySummaryDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.repository.CreditFacilityHistoryRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.FinancingTransactionRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.DocumentService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    /** Documentos que no llegaron a financiarse: el proveedor no los ve en su bitácora. */
    private static final List<DocumentStatusEnum> SUPPLIER_HIDDEN_STATUSES = List.of(
            DocumentStatusEnum.IN_QUARANTINE, DocumentStatusEnum.INACTIVATED_BY_PAYER,
            DocumentStatusEnum.REQUESTED_FOR_DISPERSION, DocumentStatusEnum.DISPERSED);

    private final DocumentRepository documentRepository;
    private final MasterAgreementRepository masterAgreementRepository;
    private final EntityRepository entityRepository;
    private final FinancingTransactionRepository financingTransactionRepository;
    private final DisbursementPolicyService disbursementPolicyService;
    private final DocumentLogRepository documentLogRepository;
    private final CreditFacilityRepository creditFacilityRepository;
    private final CreditFacilityHistoryRepository creditFacilityHistoryRepository;
    private final CurrentUserService currentUserService;

    /**
     * Antes de devolver los documentos financiables, pasa a cuarentena los
     * documentos Cargados del convenio que ya no pueden financiarse.
     */
    @Override
    @Transactional
    public List<DocumentDTOResponse> getFinanceableDocumentsByMasterAgreement(UUID masterAgreementId) {

        log.info("Fetching financeable documents for master agreement ID: {}", masterAgreementId);

        MasterAgreementModel masterAgreement = masterAgreementRepository.findById(masterAgreementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el convenio marco con ID: " + masterAgreementId));

        LocalDate minDueDate = disbursementPolicyService.financeableDueDateThreshold(
                masterAgreement.getDisbursementPolicy());

        List<DocumentModel> nonFinanceable = documentRepository.findByMasterAgreementAndStatusAndDueDateLessThanEqualForUpdate(
                masterAgreementId, DocumentStatusEnum.APPROVED, minDueDate);
        if (!nonFinanceable.isEmpty()) {
            inactivate(nonFinanceable, DocumentStatusEnum.IN_QUARANTINE, currentUserService.managed());
            log.info("{} documento(s) del convenio {} pasaron a cuarentena por no ser financiables.",
                    nonFinanceable.size(), masterAgreementId);
        }

        return documentRepository
                .findByMasterAgreementAndStatusAndDueDateAfter(
                        masterAgreementId,
                        DocumentStatusEnum.APPROVED,
                        minDueDate)
                .stream()
                .map(DocumentDTOResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentHistoryDTOResponse> getDocumentHistoryBySupplier(UUID supplierId, UUID payerId,
            DocumentStatusEnum status, String search, int page, int size) {

        log.info("Fetching document history for supplier ID: {}", supplierId);

        if (!entityRepository.existsById(supplierId)) {
            throw new ResourceNotFoundException("Proveedor no encontrado con ID: " + supplierId);
        }

        Specification<DocumentModel> spec = Specification.<DocumentModel>where(
                (root, query, cb) -> cb.equal(root.get("masterAgreement").get("supplier").get("id"), supplierId))
                .and((root, query, cb) -> cb.not(root.get("status").in(SUPPLIER_HIDDEN_STATUSES)))
                .and(payerId == null ? null
                        : (root, query, cb) -> cb.equal(root.get("masterAgreement").get("payer").get("id"), payerId))
                .and(hasStatus(status))
                .and(matches(search, false));

        Pageable pageable = PageRequests.of(page, size,
                Sort.by(Sort.Order.desc("issueDate"), Sort.Order.desc("createdAt")));

        return toHistoryPage(documentRepository.findAll(spec, pageable), false, false);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DocumentHistoryDTOResponse> getDocumentHistoryByPayer(UUID payerId, UUID supplierId,
            DocumentStatusEnum status, String search, int page, int size) {

        log.info("Fetching document history for payer ID: {}", payerId);

        if (!entityRepository.existsById(payerId)) {
            throw new ResourceNotFoundException("Pagador no encontrado con ID: " + payerId);
        }

        Specification<DocumentModel> spec = Specification.<DocumentModel>where(
                (root, query, cb) -> cb.equal(root.get("masterAgreement").get("payer").get("id"), payerId))
                .and(supplierId == null ? null
                        : (root, query, cb) -> cb.equal(root.get("masterAgreement").get("supplier").get("id"), supplierId))
                .and(hasStatus(status))
                .and(matches(search, true));

        Pageable pageable = PageRequests.of(page, size, Sort.by(
                Sort.Order.asc("masterAgreement.supplier.name"),
                Sort.Order.desc("issueDate"),
                Sort.Order.desc("createdAt")));

        // El operador bancario consulta la bitácora del pagador, pero solo el pagador inactiva.
        return toHistoryPage(documentRepository.findAll(spec, pageable), true, !currentUserService.isAdmin());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentHistorySummaryDTOResponse> getDocumentHistorySummaryByPayer(UUID payerId) {

        if (!entityRepository.existsById(payerId)) {
            throw new ResourceNotFoundException("Pagador no encontrado con ID: " + payerId);
        }

        return documentRepository.summarizeHistoryByPayer(payerId);
    }

    private static Specification<DocumentModel> hasStatus(DocumentStatusEnum status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /**
     * Búsqueda por contenido, sin distinguir mayúsculas, en el número que se muestra
     * (número de documento o, si no tiene, número de control) y opcionalmente en el
     * nombre del proveedor. Los comodines de LIKE del usuario se tratan como texto.
     */
    private static Specification<DocumentModel> matches(String search, boolean includeSupplierName) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";

        return (root, query, cb) -> {
            List<Predicate> options = new ArrayList<>();
            options.add(cb.like(cb.lower(root.get("documentNumber")), pattern, '\\'));
            options.add(cb.like(cb.lower(root.get("controlNumber")), pattern, '\\'));
            if (includeSupplierName) {
                options.add(cb.like(cb.lower(root.get("masterAgreement").get("supplier").get("name")), pattern, '\\'));
            }
            return cb.or(options.toArray(Predicate[]::new));
        };
    }

    private Page<DocumentHistoryDTOResponse> toHistoryPage(Page<DocumentModel> documents, boolean showUploader,
            boolean canInactivate) {
        return new PageImpl<>(toHistory(documents.getContent(), showUploader, canInactivate),
                documents.getPageable(), documents.getTotalElements());
    }

    /** El pagador pasa a Inactivo un documento Cargado, sea financiable o no. */
    @Override
    @Transactional
    public void inactivateDocumentByPayer(UUID documentId) {

        UserModel currentUser = currentUserService.managed();

        DocumentModel document = documentRepository.findByIdForUpdate(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + documentId));

        MasterAgreementModel agreement = document.getMasterAgreement();
        if (currentUser.getEntity() == null
                || !currentUser.getEntity().getId().equals(agreement.getPayer().getId())) {
            throw new UnauthorizedAccessException("El documento no pertenece al pagador del usuario.");
        }

        if (document.getStatus() != DocumentStatusEnum.APPROVED) {
            throw new IllegalArgumentException("Solo se pueden inactivar documentos en estado Cargado.");
        }

        inactivate(List.of(document), DocumentStatusEnum.INACTIVATED_BY_PAYER, currentUser);
        log.info("Documento {} inactivado manualmente por el usuario {}.", documentId, currentUser.getId());
    }

    @Override
    @Transactional
    public void quarantineNonFinanceable(List<DocumentModel> documents, UserModel actor) {
        if (documents.isEmpty()) {
            return;
        }
        for (DocumentModel doc : documents) {
            if (doc.getStatus() != DocumentStatusEnum.APPROVED) {
                throw new IllegalStateException("El documento " + displayNumber(doc) + " no está Cargado.");
            }
        }
        inactivate(documents, DocumentStatusEnum.IN_QUARANTINE, actor);
    }

    /**
     * Pasa los documentos al estado inactivo indicado (no financiable o inactivado
     * por el pagador) y devuelve al cupo de su pagador el monto nominal que consumió su carga.
     * El motivo solo aplica a los no financiables; en los del pagador el estado ya es el motivo.
     */
    private void inactivate(List<DocumentModel> documents, DocumentStatusEnum status, UserModel actor) {
        LocalDate today = disbursementPolicyService.businessToday();
        Instant now = Instant.now();
        List<DocumentLogModel> logs = new ArrayList<>(documents.size());

        for (DocumentModel doc : documents) {
            doc.setStatus(status);
            doc.setQuarantineReason(status == DocumentStatusEnum.IN_QUARANTINE
                    ? QuarantineReasonEnum.forUnrequested(doc.getDueDate(), today)
                    : null);
            doc.setQuarantinedAt(now);
            logs.add(DocumentLogModel.builder()
                    .document(doc)
                    .user(actor)
                    .status(status)
                    .build());
        }

        documentRepository.saveAll(documents);
        documentLogRepository.saveAll(logs);
        releaseCreditFacilities(documents, actor);
        documentRepository.flush();
    }

    private void releaseCreditFacilities(List<DocumentModel> documents, UserModel actor) {
        // Ordenado para bloquear los cupos siempre en el mismo orden.
        Map<UUID, List<DocumentModel>> byPayer = new TreeMap<>();
        for (DocumentModel doc : documents) {
            if (doc.getNominalAmount() != null && doc.getNominalAmount().signum() > 0) {
                byPayer.computeIfAbsent(doc.getMasterAgreement().getPayer().getId(), id -> new ArrayList<>()).add(doc);
            }
        }

        byPayer.forEach((payerId, payerDocuments) -> {
            CreditFacilityModel facility = creditFacilityRepository.findByPayerIdForUpdate(payerId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No se encontró el cupo de crédito del pagador con ID: " + payerId));

            BigDecimal currentUsed = facility.getAmountInUse() != null ? facility.getAmountInUse() : BigDecimal.ZERO;
            BigDecimal used = currentUsed;
            List<CreditFacilityHistoryModel> movements = new ArrayList<>(payerDocuments.size());

            for (DocumentModel doc : payerDocuments) {
                used = used.subtract(doc.getNominalAmount());
                movements.add(CreditFacilityHistoryModel.builder()
                        .amount(doc.getNominalAmount())
                        .referenceNumber(displayNumber(doc))
                        .repaymentType(RepaymentTypeEnum.DOCUMENT_INACTIVATION)
                        .creditFacility(facility)
                        .payer(facility.getPayer())
                        .executedBy(actor)
                        .build());
            }

            if (used.signum() < 0) {
                log.warn("La liberación de {} documento(s) supera el consumo del cupo {} (${}); el consumo queda en cero.",
                        payerDocuments.size(), facility.getId(), currentUsed);
                used = BigDecimal.ZERO;
            }

            facility.setAmountInUse(used);
            creditFacilityRepository.save(facility);
            creditFacilityHistoryRepository.saveAll(movements);
            log.info("Cupo {} liberado por inactivación de {} documento(s): consumo ${} -> ${}.",
                    facility.getId(), payerDocuments.size(), currentUsed, used);
        });
    }

    private static String displayNumber(DocumentModel doc) {
        return doc.getDocumentNumber() != null && !doc.getDocumentNumber().isBlank()
                ? doc.getDocumentNumber()
                : doc.getControlNumber();
    }

    /**
     * @param showUploader  incluir quién cargó el documento; el proveedor no ve a los usuarios del pagador.
     * @param canInactivate quien consulta puede pasar a Inactivo los documentos Cargados.
     */
    private List<DocumentHistoryDTOResponse> toHistory(List<DocumentModel> documents, boolean showUploader,
            boolean canInactivate) {

        List<UUID> documentIds = documents.stream().map(DocumentModel::getId).toList();

        Map<UUID, FinancingTransactionModel> transactionByDocumentId = new HashMap<>();
        if (!documentIds.isEmpty()) {
            for (FinancingTransactionModel tx : financingTransactionRepository.findByDocument_IdIn(documentIds)) {
                transactionByDocumentId.put(tx.getDocument().getId(), tx);
            }
        }

        return documents.stream()
                .map(doc -> {
                    FinancingTransactionModel tx = transactionByDocumentId.get(doc.getId());
                    BigDecimal disbursedAmount = tx != null && doc.getStatus() == DocumentStatusEnum.DISBURSED
                            ? Money.round(tx.getAmountToBeDisbursed())
                            : null;

                    boolean inactivatable = canInactivate && doc.getStatus() == DocumentStatusEnum.APPROVED;

                    return new DocumentHistoryDTOResponse(
                        doc.getId(),
                        displayNumber(doc),
                        doc.getIssueDate(),
                        doc.getDueDate(),
                        tx != null ? tx.getScheduledDisbursementDate() : null,
                        doc.getNominalAmount(),
                        disbursedAmount,
                        doc.getStatus(),
                        doc.getMasterAgreement().getPayer().getId(),
                        doc.getMasterAgreement().getPayer().getName(),
                        doc.getMasterAgreement().getSupplier().getId(),
                        doc.getMasterAgreement().getSupplier().getName(),
                        doc.getMasterAgreement().getSupplier().getNit(),
                        showUploader ? doc.getUploadBatch().getUploadedAndApprovedBy().fullName() : null,
                        inactivatable);
                })
                .toList();
    }

}
