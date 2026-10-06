package com.davivienda.factoraje.service.impl;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.dispersion.DispersionDocumentDTOResponse;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;

import lombok.RequiredArgsConstructor;

/**
 * Qué documentos se pueden dispersar y cómo se muestran. Se dispersan los que el
 * proveedor no anticipó: los que están en cuarentena y los Cargados que ya no son
 * financiables porque el proveedor no entró a la plataforma para que pasaran a cuarentena.
 */
@Component
@RequiredArgsConstructor
public class DispersionDocuments {

    static final Comparator<DocumentModel> LETTER_ORDER = Comparator
            .comparing((DocumentModel d) -> d.getMasterAgreement().getSupplier().getName(),
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(DocumentModel::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(DocumentModel::getId);

    private final DocumentRepository documentRepository;
    private final DisbursementPolicyCatRepository disbursementPolicyCatRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DisbursementPolicyService disbursementPolicyService;

    /** Documentos sin lote de dispersión que se pueden dispersar, de un pagador. */
    public List<DocumentModel> pendingOfPayer(UUID payerId) {
        return eligible(documentRepository.findDispersionCandidatesByPayer(payerId,
                DocumentStatusEnum.IN_QUARANTINE, DocumentStatusEnum.APPROVED, latestThreshold()));
    }

    /** Documentos sin lote de dispersión que se pueden dispersar, de todos los pagadores. */
    public List<DocumentModel> pendingOfAllPayers() {
        return eligible(documentRepository.findDispersionCandidates(
                DocumentStatusEnum.IN_QUARANTINE, DocumentStatusEnum.APPROVED, latestThreshold()));
    }

    /** Descarta los Cargados que aún son financiables; los de cuarentena siempre se pueden dispersar. */
    public List<DocumentModel> eligible(Collection<DocumentModel> candidates) {
        Map<DisbursementPolicyCat, LocalDate> thresholdByPolicy = new IdentityHashMap<>();
        return candidates.stream()
                .filter(doc -> doc.getStatus() == DocumentStatusEnum.IN_QUARANTINE
                        || (doc.getStatus() == DocumentStatusEnum.APPROVED
                                && !doc.getDueDate().isAfter(threshold(doc, thresholdByPolicy))))
                .toList();
    }

    /** Cuenta principal de cada entidad; las que no tienen no aparecen en el mapa. */
    public Map<UUID, String> mainAccounts(Collection<UUID> entityIds) {
        Map<UUID, String> accounts = new HashMap<>();
        if (!entityIds.isEmpty()) {
            bankAccountRepository.findAllByEntityModelIdInAndIsMainTrue(entityIds)
                    .forEach(account -> accounts.putIfAbsent(account.getEntityModel().getId(),
                            account.getAccountNumber()));
        }
        return accounts;
    }

    public List<DispersionDocumentDTOResponse> toResponses(List<DocumentModel> documents) {
        Map<UUID, String> accounts = mainAccounts(documents.stream()
                .map(doc -> doc.getMasterAgreement().getSupplier().getId())
                .distinct()
                .toList());

        return documents.stream()
                .sorted(LETTER_ORDER)
                .map(doc -> {
                    EntityModel supplier = doc.getMasterAgreement().getSupplier();
                    return new DispersionDocumentDTOResponse(
                            doc.getId(),
                            dteNumber(doc),
                            supplier.getId(),
                            supplier.getName(),
                            accounts.get(supplier.getId()),
                            doc.getDueDate(),
                            Money.round(doc.getNominalAmount()),
                            doc.getStatus());
                })
                .toList();
    }

    /** Número de control del DTE; los documentos en papel no tienen y usan su número de documento. */
    static String dteNumber(DocumentModel doc) {
        return doc.getControlNumber() != null && !doc.getControlNumber().isBlank()
                ? doc.getControlNumber()
                : doc.getDocumentNumber();
    }

    private LocalDate threshold(DocumentModel doc, Map<DisbursementPolicyCat, LocalDate> cache) {
        DisbursementPolicyCat policy = doc.getMasterAgreement().getDisbursementPolicy();
        if (policy == null) {
            return disbursementPolicyService.financeableDueDateThreshold();
        }
        return cache.computeIfAbsent(policy, disbursementPolicyService::financeableDueDateThreshold);
    }

    /** El mayor límite de financiabilidad entre las políticas, para acotar la consulta. */
    private LocalDate latestThreshold() {
        LocalDate latest = disbursementPolicyService.financeableDueDateThreshold();
        for (DisbursementPolicyCat policy : disbursementPolicyCatRepository.findAll()) {
            LocalDate threshold = disbursementPolicyService.financeableDueDateThreshold(policy);
            if (threshold.isAfter(latest)) {
                latest = threshold;
            }
        }
        return latest;
    }
}
