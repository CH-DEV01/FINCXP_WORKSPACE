package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.entities.DispersionBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchDetailDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchHistoryDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchSummaryDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionDocumentDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionPayerDTOResponse;
import com.davivienda.factoraje.dto.dispersion.DispersionRequestDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.repository.DispersionBatchRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.service.DispersionQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DispersionQueryServiceImpl implements DispersionQueryService {

    private final EntityRepository entityRepository;
    private final DocumentRepository documentRepository;
    private final DispersionBatchRepository dispersionBatchRepository;
    private final DispersionDocuments dispersionDocuments;

    @Override
    @Transactional(readOnly = true)
    public List<DispersionPayerDTOResponse> getPayers() {

        List<EntityModel> payers = entityRepository.findAllByEntityTypeCodeOrderByName(EntityTypeCode.PAYER);
        if (payers.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> accounts = dispersionDocuments.mainAccounts(payers.stream().map(EntityModel::getId).toList());

        Map<UUID, Set<LocalDate>> pendingDueDatesByPayer = new HashMap<>();
        for (DocumentModel doc : dispersionDocuments.pendingOfAllPayers()) {
            pendingDueDatesByPayer
                    .computeIfAbsent(doc.getMasterAgreement().getPayer().getId(), id -> new HashSet<>())
                    .add(doc.getDueDate());
        }

        return payers.stream()
                .map(payer -> new DispersionPayerDTOResponse(
                        payer.getId(),
                        payer.getName(),
                        accounts.get(payer.getId()),
                        pendingDueDatesByPayer.getOrDefault(payer.getId(), Set.of()).size()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispersionRequestDTOResponse> getPayerRequests(UUID payerId) {

        requirePayer(payerId);

        Map<LocalDate, List<DocumentModel>> pendingByDueDate = new TreeMap<>();
        for (DocumentModel doc : dispersionDocuments.pendingOfPayer(payerId)) {
            pendingByDueDate.computeIfAbsent(doc.getDueDate(), d -> new ArrayList<>()).add(doc);
        }

        List<DispersionRequestDTOResponse> requests = new ArrayList<>();
        pendingByDueDate.forEach((dueDate, documents) -> requests.add(DispersionRequestDTOResponse.pending(
                dueDate,
                documents.size(),
                documents.stream().map(doc -> doc.getMasterAgreement().getSupplier().getId()).distinct().count(),
                documents.stream().map(DocumentModel::getNominalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))));

        List<DispersionBatchModel> batches = dispersionBatchRepository.findAllByPayerWithUsersOrderByCreatedAtDesc(payerId);
        Map<UUID, Long> suppliersByBatch = suppliersByBatch(batches);
        batches.forEach(batch -> requests.add(DispersionRequestDTOResponse.fromBatch(
                batch, suppliersByBatch.getOrDefault(batch.getId(), 0L))));

        return requests;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DispersionDocumentDTOResponse> getPendingDocuments(UUID payerId, LocalDate dueDate) {

        requirePayer(payerId);

        return dispersionDocuments.toResponses(dispersionDocuments.pendingOfPayer(payerId).stream()
                .filter(doc -> dueDate.equals(doc.getDueDate()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DispersionBatchHistoryDTOResponse getBatchHistory(UUID payerId, int page, int size) {

        Pageable pageable = PageRequests.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<DispersionBatchModel> batches = payerId == null
                ? dispersionBatchRepository.findPageWithUsers(pageable)
                : dispersionBatchRepository.findPageByPayerWithUsers(payerId, pageable);

        BigDecimal totalAmount = payerId == null
                ? dispersionBatchRepository.sumTotalAmount()
                : dispersionBatchRepository.sumTotalAmountByPayer(payerId);

        return new DispersionBatchHistoryDTOResponse(
                batches.getContent().stream().map(DispersionBatchSummaryDTOResponse::fromEntity).toList(),
                batches.getNumber(),
                batches.getSize(),
                batches.getTotalPages(),
                batches.getTotalElements(),
                Money.roundOrZero(totalAmount));
    }

    @Override
    @Transactional(readOnly = true)
    public DispersionBatchDetailDTOResponse getBatchDetails(UUID batchId) {

        DispersionBatchModel batch = dispersionBatchRepository.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de dispersión no encontrado con ID: " + batchId));

        return new DispersionBatchDetailDTOResponse(
                DispersionBatchSummaryDTOResponse.fromEntity(batch),
                DispersionBatchSummaryDTOResponse.fullName(batch.getSigner()),
                dispersionDocuments.toResponses(documentRepository.findByDispersionBatchIdWithDetails(batchId)));
    }

    private Map<UUID, Long> suppliersByBatch(List<DispersionBatchModel> batches) {
        Map<UUID, Long> suppliers = new HashMap<>();
        if (!batches.isEmpty()) {
            for (Object[] row : documentRepository.countSuppliersByDispersionBatch(
                    batches.stream().map(DispersionBatchModel::getId).toList())) {
                suppliers.put((UUID) row[0], ((Number) row[1]).longValue());
            }
        }
        return suppliers;
    }

    private void requirePayer(UUID payerId) {
        if (!entityRepository.existsById(payerId)) {
            throw new ResourceNotFoundException("Pagador no encontrado con ID: " + payerId);
        }
    }
}
