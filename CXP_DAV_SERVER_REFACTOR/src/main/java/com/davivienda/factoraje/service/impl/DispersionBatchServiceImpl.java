package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.DispersionBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentLogModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.BatchTypeEnum;
import com.davivienda.factoraje.domain.enums.DispersionBatchStatusEnum;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchSummaryDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.DocumentNotAvailableException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.util.BatchNumberGeneratorUtil;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.DispersionBatchRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.DispersionBatchService;
import com.davivienda.factoraje.service.DispersionLetterPdfService;
import com.davivienda.factoraje.service.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispersionBatchServiceImpl implements DispersionBatchService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserRepository userRepository;
    private final EntityRepository entityRepository;
    private final BankAccountRepository bankAccountRepository;
    private final DocumentRepository documentRepository;
    private final DocumentLogRepository documentLogRepository;
    private final DispersionBatchRepository dispersionBatchRepository;
    private final DisbursementPolicyService disbursementPolicyService;
    private final DocumentService documentService;
    private final DispersionDocuments dispersionDocuments;
    private final DispersionLetterAssembler letterAssembler;
    private final DispersionLetterPdfService letterPdfService;
    private final MailNoticePublisher mailNotices;

    @Override
    @Transactional
    public Letter generateBatch(UUID createdById, UUID payerId, LocalDate dueDate) {

        UserModel createdBy = findUser(createdById);

        // Serializa la generación por pagador: una segunda petición simultánea espera y,
        // al buscar los documentos sin lote, ya no encuentra los que tomó la primera.
        EntityModel payer = entityRepository.findByIdForUpdate(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagador no encontrado con ID: " + payerId));

        String payerAccount = bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(payerId)
                .map(BankAccountModel::getAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("El pagador " + payer.getName()
                        + " no tiene una cuenta principal a la que cargar la dispersión."));

        List<DocumentModel> documents = dispersionDocuments.eligible(documentRepository.findWithoutDispersionBatchForUpdate(
                payerId, dueDate, List.of(DocumentStatusEnum.IN_QUARANTINE, DocumentStatusEnum.APPROVED)));
        if (documents.isEmpty()) {
            throw new DocumentNotAvailableException("El pagador " + payer.getName()
                    + " ya no tiene documentos por dispersar con vencimiento " + DATE_FMT.format(dueDate) + ".");
        }

        // Los Cargados que ya no son financiables pasan primero por cuarentena, igual que
        // cuando el proveedor entra a la plataforma: así se libera su monto del cupo.
        documentService.quarantineNonFinanceable(documents.stream()
                .filter(doc -> doc.getStatus() == DocumentStatusEnum.APPROVED)
                .toList(), createdBy);

        DispersionBatchModel batch = dispersionBatchRepository.save(DispersionBatchModel.builder()
                .batchNumber(uniqueBatchNumber())
                .payer(payer)
                .payerAccountNumber(payerAccount)
                .dueDate(dueDate)
                // Se dispersa el mismo día del vencimiento; el operador debe generar el lote a tiempo.
                .dispersionDate(dueDate)
                .documentCount(documents.size())
                .totalAmount(documents.stream().map(DocumentModel::getNominalAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .status(DispersionBatchStatusEnum.CREATED)
                .signer(DispersionLetterAssembler.signerOf(documents))
                .createdBy(createdBy)
                .build());

        changeStatus(documents, DocumentStatusEnum.REQUESTED_FOR_DISPERSION, createdBy);
        documents.forEach(doc -> doc.setDispersionBatch(batch));
        documentRepository.saveAll(documents);

        log.info("Lote de dispersión {} creado: pagador {}, vencimiento {}, {} documento(s), total {}.",
                batch.getBatchNumber(), payerId, dueDate, documents.size(), batch.getTotalAmount());

        Letter result = letter(batch, documents);
        mailNotices.dispersionBatchCreated(batch.getBatchNumber(), payer.getName(), dueDate, batch.getDocumentCount());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Letter regenerateLetter(UUID batchId) {

        DispersionBatchModel batch = dispersionBatchRepository.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de dispersión no encontrado con ID: " + batchId));

        return letter(batch, documentRepository.findByDispersionBatchIdWithDetails(batchId));
    }

    @Override
    @Transactional
    public DispersionBatchSummaryDTOResponse confirmBatch(UUID batchId, UUID confirmedById) {

        DispersionBatchModel batch = dispersionBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote de dispersión no encontrado con ID: " + batchId));

        if (batch.getStatus() != DispersionBatchStatusEnum.CREATED) {
            throw new DocumentNotAvailableException("El lote " + batch.getBatchNumber() + " ya fue confirmado.");
        }

        UserModel confirmedBy = findUser(confirmedById);

        List<DocumentModel> documents = documentRepository.findByDispersionBatchIdForUpdate(batchId);
        for (DocumentModel doc : documents) {
            if (doc.getStatus() != DocumentStatusEnum.REQUESTED_FOR_DISPERSION) {
                throw new DocumentNotAvailableException("El documento " + DispersionDocuments.dteNumber(doc)
                        + " del lote " + batch.getBatchNumber() + " no está en dispersión.");
            }
        }

        changeStatus(documents, DocumentStatusEnum.DISPERSED, confirmedBy);
        documentRepository.saveAll(documents);

        batch.setStatus(DispersionBatchStatusEnum.SETTLED);
        batch.setConfirmedBy(confirmedBy);
        batch.setConfirmedAt(Instant.now());
        DispersionBatchModel saved = dispersionBatchRepository.save(batch);

        log.info("Lote de dispersión {} confirmado por el usuario {}: {} documento(s) dispersados.",
                batch.getBatchNumber(), confirmedById, documents.size());

        mailNotices.operatorChanged("Dispersiones",
                "Lote " + batch.getBatchNumber() + ": Creado",
                "Lote " + batch.getBatchNumber() + ": Liquidado (" + documents.size() + " documento(s) dispersados)");
        return DispersionBatchSummaryDTOResponse.fromEntity(saved);
    }

    private void changeStatus(List<DocumentModel> documents, DocumentStatusEnum status, UserModel user) {
        List<DocumentLogModel> logs = new ArrayList<>(documents.size());
        for (DocumentModel doc : documents) {
            doc.setStatus(status);
            logs.add(DocumentLogModel.builder().document(doc).status(status).user(user).build());
        }
        documentLogRepository.saveAll(logs);
    }

    private Letter letter(DispersionBatchModel batch, List<DocumentModel> documents) {
        return new Letter(letterAssembler.fileName(batch),
                letterPdfService.generate(letterAssembler.assemble(batch, documents)));
    }

    private String uniqueBatchNumber() {
        LocalDate today = disbursementPolicyService.businessToday();
        String batchNumber;
        do {
            batchNumber = BatchNumberGeneratorUtil.generateBatchNumber(BatchTypeEnum.DISPERSION, today);
        } while (dispersionBatchRepository.existsByBatchNumber(batchNumber));
        return batchNumber;
    }

    private UserModel findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
    }
}
