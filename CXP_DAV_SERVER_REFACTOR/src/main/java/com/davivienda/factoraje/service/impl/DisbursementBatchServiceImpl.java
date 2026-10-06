package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentLogModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.BatchTypeEnum;
import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.disbursement_batch.GenerateBatchRequestDTO;
import com.davivienda.factoraje.infrastructure.exception.DocumentNotAvailableException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.util.BatchNumberGeneratorUtil;
import com.davivienda.factoraje.infrastructure.util.FileNamingUtil;
import com.davivienda.factoraje.repository.DisbursementBatchRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.FinancingTransactionRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.DisbursementBatchService;
import com.davivienda.factoraje.service.DisbursementLetterPdfService;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.impl.DisbursementLetterAssembler.ReportContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisbursementBatchServiceImpl implements DisbursementBatchService {

    private final UserRepository userRepository;
    private final EntityRepository entityRepository;
    private final DocumentRepository documentRepository;
    private final DocumentLogRepository documentLogRepository;
    private final FinancingTransactionRepository financingTransactionRepository;
    private final DisbursementBatchRepository disbursementBatchRepository;
    private final DisbursementPolicyService disbursementPolicyService;
    private final DisbursementLetterAssembler letterAssembler;
    private final DisbursementLetterPdfService disbursementLetterPdfService;
    private final MailNoticePublisher mailNotices;

    @Override
    @Transactional
    public Map<String, byte[]> generateDisbursementBatches(UUID createdById, String originalFileName, UUID payerId,
            List<GenerateBatchRequestDTO.Group> groups) {

        UserModel createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + createdById));

        // Serializa la generación por pagador: una segunda petición simultánea espera y, al
        // consultar las pendientes, ya no encuentra las transacciones que tomó la primera.
        EntityModel payer = entityRepository.findByIdForUpdate(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagador no encontrado con ID: " + payerId));

        List<GenerateBatchRequestDTO.Group> selectedGroups = groups == null
                ? List.of()
                : groups.stream().distinct().toList();
        if (selectedGroups.isEmpty()) {
            throw new IllegalArgumentException("No se seleccionó ninguna solicitud para generar lotes.");
        }

        ReportContext reportContext = letterAssembler.loadReportContext(payer);
        Map<String, byte[]> reports = new LinkedHashMap<>();

        for (GenerateBatchRequestDTO.Group group : selectedGroups) {
            List<FinancingTransactionModel> transactions = financingTransactionRepository
                    .findPendingInGroup(payerId, DocumentStatusEnum.REQUESTED_FOR_FINANCING,
                            group.dueDate(), group.requestDate(), group.disbursementDate());

            if (transactions.isEmpty()) {
                log.warn("La combinación vencimiento {} / solicitud {} / desembolso {} del pagador {} ya no tiene documentos pendientes.",
                        group.dueDate(), group.requestDate(), group.disbursementDate(), payerId);
                continue;
            }

            DisbursementBatchModel batch = createBatch(transactions, group, payer, createdBy, originalFileName);
            reports.put(letterAssembler.reportFileName(batch), buildBatchPdf(batch, transactions, reportContext));
            mailNotices.disbursementBatchCreated(batch.getBatchNumber(), payer.getName(), batch.getDueDate(),
                    batch.getRequestDate(), batch.getDisbursementDate(), batch.getTransactionCount());
        }

        if (reports.isEmpty()) {
            throw new DocumentNotAvailableException(
                    "Las solicitudes seleccionadas ya no tienen documentos listos para desembolso.");
        }

        log.info("Se generaron {} lote(s) de desembolso para el pagador {}.", reports.size(), payerId);

        return reports;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, byte[]> regenerateBatchReport(UUID batchId) {

        DisbursementBatchModel batch = disbursementBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de desembolso no encontrado con ID: " + batchId));

        List<FinancingTransactionModel> transactions = financingTransactionRepository
                .findByDisbursementBatchIdWithDocuments(batchId);

        if (transactions.isEmpty()) {
            throw new DocumentNotAvailableException(
                    "El lote " + batch.getBatchNumber() + " no tiene transacciones asociadas.");
        }

        EntityModel payer = batch.getPayer() != null
                ? batch.getPayer()
                : transactions.getFirst().getDocument().getMasterAgreement().getPayer();

        Map<String, byte[]> reports = new LinkedHashMap<>();
        reports.put(letterAssembler.reportFileName(batch),
                buildBatchPdf(batch, transactions, letterAssembler.loadReportContext(payer)));
        return reports;
    }

    private byte[] buildBatchPdf(DisbursementBatchModel batch, List<FinancingTransactionModel> transactions,
            ReportContext context) {
        return disbursementLetterPdfService.generate(letterAssembler.assemble(batch, transactions, context));
    }

    private DisbursementBatchModel createBatch(List<FinancingTransactionModel> transactions,
            GenerateBatchRequestDTO.Group group, EntityModel payer, UserModel createdBy, String originalFileName) {

        String batchNumber = uniqueBatchNumber();

        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal totalToDisburse = BigDecimal.ZERO;
        for (FinancingTransactionModel tx : transactions) {
            totalInterest = totalInterest.add(tx.getInterestAmount());
            totalCommission = totalCommission.add(tx.getCommissionAmount());
            totalToDisburse = totalToDisburse.add(tx.getAmountToBeDisbursed());
        }

        DisbursementBatchModel batch = disbursementBatchRepository.save(DisbursementBatchModel.builder()
                .batchNumber(batchNumber)
                .outputFileName(FileNamingUtil.generateBatchFileName(batchNumber, originalFileName))
                .transactionCount(transactions.size())
                .totalAmount(totalToDisburse)
                .totalCommission(totalCommission)
                .totalInterest(totalInterest)
                .status(DisbursementBatchStatusEnum.CREATED)
                .createdBy(createdBy)
                .payer(payer)
                .dueDate(group.dueDate())
                .requestDate(group.requestDate())
                .disbursementDate(group.disbursementDate())
                .build());

        List<DocumentModel> documents = new ArrayList<>(transactions.size());
        List<DocumentLogModel> documentLogs = new ArrayList<>(transactions.size());

        for (FinancingTransactionModel tx : transactions) {
            tx.setDisbursementBatch(batch);

            DocumentModel doc = tx.getDocument();
            doc.setStatus(DocumentStatusEnum.REQUESTED_FOR_DISBURSEMENT);
            documents.add(doc);
            documentLogs.add(DocumentLogModel.builder()
                    .document(doc)
                    .status(DocumentStatusEnum.REQUESTED_FOR_DISBURSEMENT)
                    .user(createdBy)
                    .build());
        }

        documentRepository.saveAll(documents);
        financingTransactionRepository.saveAll(transactions);
        documentLogRepository.saveAll(documentLogs);

        log.info("Lote {} creado: {} documentos, vencimiento {}, solicitud {}, desembolso {}, total a desembolsar {}.",
                batchNumber, transactions.size(), group.dueDate(), group.requestDate(), group.disbursementDate(),
                totalToDisburse);

        return batch;
    }

    private String uniqueBatchNumber() {
        LocalDate today = disbursementPolicyService.businessToday();
        String batchNumber;
        do {
            batchNumber = BatchNumberGeneratorUtil.generateBatchNumber(BatchTypeEnum.DISBURSEMENT, today);
        } while (disbursementBatchRepository.existsByBatchNumber(batchNumber));
        return batchNumber;
    }
}
