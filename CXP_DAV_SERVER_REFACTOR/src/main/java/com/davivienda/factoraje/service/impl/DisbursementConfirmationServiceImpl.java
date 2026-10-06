package com.davivienda.factoraje.service.impl;

import static com.davivienda.factoraje.service.impl.DisbursementSummaries.groupBySupplier;
import static com.davivienda.factoraje.service.impl.DisbursementSummaries.sumCents;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentLogModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementBatchSummaryDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.DocumentNotAvailableException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.repository.DisbursementBatchRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.FinancingTransactionRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.DisbursementConfirmationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisbursementConfirmationServiceImpl implements DisbursementConfirmationService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentLogRepository documentLogRepository;
    private final FinancingTransactionRepository financingTransactionRepository;
    private final DisbursementBatchRepository disbursementBatchRepository;
    private final MailNoticePublisher mailNotices;

    @Override
    @Transactional
    public DisbursementBatchSummaryDTOResponse confirmDisbursementBatch(UUID batchId, UUID confirmedById) {

        DisbursementBatchModel batch = disbursementBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de desembolso no encontrado con ID: " + batchId));

        if (batch.getStatus() != DisbursementBatchStatusEnum.CREATED
                && batch.getStatus() != DisbursementBatchStatusEnum.PROCESSING) {
            throw new DocumentNotAvailableException(
                    "El lote " + batch.getBatchNumber() + " ya fue confirmado (estado: " + batch.getStatus() + ").");
        }

        UserModel confirmedBy = findUser(confirmedById);

        List<FinancingTransactionModel> transactions = financingTransactionRepository
                .findByDisbursementBatchIdWithDocuments(batchId);

        if (transactions.isEmpty()) {
            throw new DocumentNotAvailableException(
                    "El lote " + batch.getBatchNumber() + " no tiene documentos asociados.");
        }

        List<UUID> documentIds = transactions.stream()
                .map(tx -> tx.getDocument().getId())
                .toList();

        markDisbursed(documentIds, confirmedBy);

        String previous = "Lote " + batch.getBatchNumber() + ": " + statusLabel(batch.getStatus());
        batch.setStatus(DisbursementBatchStatusEnum.SETTLED);
        batch.setConfirmedBy(confirmedBy);
        DisbursementBatchModel saved = disbursementBatchRepository.save(batch);

        log.info("Lote {} confirmado completamente: {} documentos desembolsados.",
                batch.getBatchNumber(), documentIds.size());
        mailNotices.operatorChanged("Desembolsos", previous,
                "Lote " + batch.getBatchNumber() + ": " + statusLabel(DisbursementBatchStatusEnum.SETTLED)
                        + " (" + documentIds.size() + " documento(s) desembolsados)");

        BigDecimal amountToDisburse = groupBySupplier(transactions).values().stream()
                .map(txs -> sumCents(txs, FinancingTransactionModel::getAmountToFinance))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DisbursementBatchSummaryDTOResponse.fromEntity(saved, amountToDisburse);
    }

    @Override
    @Transactional
    public void confirmDisbursement(List<UUID> documentIds, UUID confirmedById) {
        markDisbursed(documentIds, findUser(confirmedById));
        mailNotices.operatorChanged("Desembolsos",
                documentIds.size() + " documento(s) pendientes de desembolso",
                documentIds.size() + " documento(s) desembolsados");
    }

    private static String statusLabel(DisbursementBatchStatusEnum status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case CREATED -> "Creado";
            case PROCESSING -> "En proceso";
            case SETTLED -> "Liquidado";
            case FAILED -> "Fallido";
            case PARTIALLY_FAILED -> "Parcialmente fallido";
        };
    }

    private void markDisbursed(List<UUID> documentIds, UserModel confirmedBy) {

        List<DocumentModel> documents = documentRepository.findAllByIdForUpdate(documentIds);

        if (documents.size() != documentIds.size()) {
            log.warn("Discrepancia en confirmación: se solicitaron {} documentos, pero se encontraron {}.",
                    documentIds.size(), documents.size());
            throw new ResourceNotFoundException("Uno o más documentos solicitados no existen en la base de datos.");
        }

        List<DocumentLogModel> documentLogs = new ArrayList<>(documents.size());

        for (DocumentModel doc : documents) {
            if (doc.getStatus() != DocumentStatusEnum.REQUESTED_FOR_DISBURSEMENT) {
                log.warn("Intento de confirmación inválido. Documento {} en estado {}.",
                        doc.getDocumentNumber(), doc.getStatus());
                throw new DocumentNotAvailableException("El documento " + doc.getDocumentNumber() +
                        " no está listo para ser desembolsado.");
            }

            documentLogs.add(DocumentLogModel.builder()
                    .document(doc)
                    .user(confirmedBy)
                    .status(DocumentStatusEnum.DISBURSED)
                    .build());
            doc.setStatus(DocumentStatusEnum.DISBURSED);
        }

        documentRepository.saveAll(documents);
        documentLogRepository.saveAll(documentLogs);

        log.info("Confirmación exitosa: se desembolsaron {} documentos por el usuario {}.",
                documents.size(), confirmedBy.getId());
    }

    private UserModel findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
    }
}
