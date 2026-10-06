package com.davivienda.factoraje.service.impl;

import static com.davivienda.factoraje.service.impl.DisbursementSummaries.cents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchDetailDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchDocumentDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchHistoryDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchSummaryDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementGroupDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestSupplierDTOResponse;
import com.davivienda.factoraje.dto.disbursement_batch.PayerDisbursementResumeDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.infrastructure.util.PageRequests;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.DisbursementBatchRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.FinancingTransactionRepository;
import com.davivienda.factoraje.service.DisbursementQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DisbursementQueryServiceImpl implements DisbursementQueryService {

    private final EntityRepository entityRepository;
    private final CreditFacilityRepository creditFacilityRepository;
    private final FinancingTransactionRepository financingTransactionRepository;
    private final DisbursementBatchRepository disbursementBatchRepository;
    private final DisbursementSummaries summaries;

    @Override
    @Transactional(readOnly = true)
    public List<PayerDisbursementResumeDTOResponse> getPayersResume() {

        List<EntityModel> payers = entityRepository.findAllByEntityTypeCodeOrderByName(EntityTypeCode.PAYER);
        if (payers.isEmpty()) {
            return List.of();
        }

        Map<UUID, CreditFacilityModel> creditFacilityByPayer = new HashMap<>();
        creditFacilityRepository.findAllByPayerIdIn(payers.stream().map(EntityModel::getId).toList())
                .forEach(facility -> creditFacilityByPayer.putIfAbsent(facility.getPayer().getId(), facility));

        Map<UUID, Long> groupsByPayer = new HashMap<>();
        for (Object[] row : financingTransactionRepository.findPendingGroupsOfAllPayers(
                DocumentStatusEnum.REQUESTED_FOR_FINANCING)) {
            groupsByPayer.merge((UUID) row[0], 1L, Long::sum);
        }

        return payers.stream().map(payer -> {
            UUID payerId = payer.getId();
            CreditFacilityModel creditFacility = creditFacilityByPayer.get(payerId);
            long availableRequests = groupsByPayer.getOrDefault(payerId, 0L);

            return new PayerDisbursementResumeDTOResponse(
                    payerId,
                    payer.getName(),
                    creditFacility != null ? creditFacility.getCreditFacilityNumber() : null,
                    creditFacility != null ? creditFacility.getAvailableAmount() : null,
                    availableRequests);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DisbursementBatchHistoryDTOResponse getBatchHistory(UUID payerId, int page, int size) {
        Pageable pageable = PageRequests.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<DisbursementBatchModel> batches = payerId == null
                ? disbursementBatchRepository.findPageWithUsers(pageable)
                : disbursementBatchRepository.findPageByPayerWithUsers(payerId, pageable);

        BigDecimal totalAmountToDisburse = payerId == null
                ? financingTransactionRepository.sumAmountToDisburseOfAllBatches()
                : financingTransactionRepository.sumAmountToDisburseOfPayerBatches(payerId);

        Map<UUID, BigDecimal> amountToDisburseByBatch = new HashMap<>();
        if (batches.hasContent()) {
            for (Object[] row : financingTransactionRepository.sumBatchesBySupplier(
                    batches.map(DisbursementBatchModel::getId).getContent())) {
                amountToDisburseByBatch.merge((UUID) row[0], cents((BigDecimal) row[2]), BigDecimal::add);
            }
        }

        List<DisbursementBatchSummaryDTOResponse> content = batches.getContent().stream()
                .map(batch -> DisbursementBatchSummaryDTOResponse.fromEntity(
                        batch, amountToDisburseByBatch.getOrDefault(batch.getId(), BigDecimal.ZERO)))
                .toList();

        return new DisbursementBatchHistoryDTOResponse(
                content,
                batches.getNumber(),
                batches.getSize(),
                batches.getTotalPages(),
                batches.getTotalElements(),
                cents(totalAmountToDisburse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisbursementRequestDTOResponse> getPayerRequests(UUID payerId) {

        List<DisbursementRequestDTOResponse> requests = new ArrayList<>();

        getDisbursementGroups(payerId).stream()
                .map(DisbursementRequestDTOResponse::fromGroup)
                .forEach(requests::add);

        Map<UUID, Long> suppliersByBatch = new HashMap<>();
        Map<UUID, BigDecimal> amountToDisburseByBatch = new HashMap<>();
        for (Object[] row : financingTransactionRepository.sumBatchesBySupplierForPayer(payerId)) {
            UUID batchId = (UUID) row[0];
            suppliersByBatch.merge(batchId, 1L, Long::sum);
            amountToDisburseByBatch.merge(batchId, cents((BigDecimal) row[2]), BigDecimal::add);
        }

        disbursementBatchRepository.findAllByPayerWithUsersOrderByCreatedAtDesc(payerId).stream()
                .map(batch -> DisbursementRequestDTOResponse.fromBatch(
                        batch,
                        suppliersByBatch.getOrDefault(batch.getId(), 0L),
                        amountToDisburseByBatch.getOrDefault(batch.getId(), BigDecimal.ZERO)))
                .forEach(requests::add);

        return requests;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisbursementRequestSupplierDTOResponse> getRequestSuppliers(UUID payerId, UUID batchId,
            LocalDate dueDate, LocalDate requestDate, LocalDate disbursementDate) {

        List<FinancingTransactionModel> transactions;
        if (batchId != null) {
            transactions = financingTransactionRepository.findByDisbursementBatchIdWithDocuments(batchId);
        } else {
            if (dueDate == null || requestDate == null || disbursementDate == null) {
                throw new IllegalArgumentException(
                        "Debe indicar el lote o las fechas de vencimiento, solicitud y desembolso de la solicitud.");
            }
            transactions = financingTransactionRepository.findPendingInGroup(
                    payerId, DocumentStatusEnum.REQUESTED_FOR_FINANCING, dueDate, requestDate, disbursementDate);
        }

        return summaries.summarizeBySupplier(transactions);
    }

    @Override
    @Transactional(readOnly = true)
    public DisbursementBatchDetailDTOResponse getBatchDetails(UUID batchId) {

        DisbursementBatchModel batch = disbursementBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de desembolso no encontrado con ID: " + batchId));

        List<FinancingTransactionModel> transactions = financingTransactionRepository
                .findByDisbursementBatchIdWithDocuments(batchId);

        String payerName = transactions.isEmpty()
                ? null
                : transactions.getFirst().getDocument().getMasterAgreement().getPayer().getName();

        List<DisbursementBatchDocumentDTOResponse> documents = transactions.stream()
                .map(DisbursementBatchDocumentDTOResponse::fromTransaction)
                .toList();

        return new DisbursementBatchDetailDTOResponse(
                batch.getId(),
                batch.getBatchNumber(),
                batch.getOutputFileName(),
                batch.getTransactionCount(),
                Money.round(batch.getTotalAmount()),
                Money.round(batch.getTotalCommission()),
                Money.round(batch.getTotalInterest()),
                batch.getStatus(),
                payerName,
                batch.getCreatedAt(),
                summaries.disbursementDate(batch, transactions),
                batch.getDueDate(),
                batch.getRequestDate(),
                documents);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisbursementGroupDTOResponse> getDisbursementGroups(UUID payerId) {

        if (!entityRepository.existsById(payerId)) {
            throw new ResourceNotFoundException("Pagador no encontrado con ID: " + payerId);
        }

        Map<GroupKey, List<Object[]>> rowsByGroup = new LinkedHashMap<>();
        for (Object[] row : financingTransactionRepository.sumPendingGroupsBySupplier(
                payerId, DocumentStatusEnum.REQUESTED_FOR_FINANCING)) {
            GroupKey key = new GroupKey((LocalDate) row[0], (LocalDate) row[1], (LocalDate) row[2]);
            rowsByGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        return rowsByGroup.entrySet().stream()
                .map(entry -> {
                    long documents = 0;
                    BigDecimal nominal = BigDecimal.ZERO;
                    BigDecimal toDisburse = BigDecimal.ZERO;
                    for (Object[] row : entry.getValue()) {
                        documents += ((Number) row[4]).longValue();
                        nominal = nominal.add((BigDecimal) row[5]);
                        toDisburse = toDisburse.add(cents((BigDecimal) row[6]));
                    }
                    GroupKey key = entry.getKey();
                    return new DisbursementGroupDTOResponse(key.dueDate(), key.requestDate(), key.disbursementDate(),
                            documents, entry.getValue().size(), nominal, toDisburse);
                })
                .toList();
    }

    private record GroupKey(LocalDate dueDate, LocalDate requestDate, LocalDate disbursementDate) {}
}
