package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;
import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.enums.AgreementTypeEnum;
import com.davivienda.factoraje.domain.enums.BatchTypeEnum;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.domain.enums.InvoiceTypeEnum;
import com.davivienda.factoraje.domain.enums.IssuanceMethodEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.domain.enums.UploadBatchStatusEnum;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTORequest;
import com.davivienda.factoraje.dto.acceptance_audit.AcceptanceAuditDTOResponse;
import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.dto.upload_batch.UploadBatchDTORequest;
import com.davivienda.factoraje.dto.upload_batch.UploadBatchDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.CreditLimitExceededException;
import com.davivienda.factoraje.infrastructure.exception.InactiveResourceException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.util.BatchNumberGeneratorUtil;
import com.davivienda.factoraje.infrastructure.util.FileNamingUtil;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.repository.PaymentPolicyCatRepository;
import com.davivienda.factoraje.service.AcceptanceAuditService;
import com.davivienda.factoraje.service.BatchPersistenceService;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.DocumentLogService;
import com.davivienda.factoraje.service.EntityService;
import com.davivienda.factoraje.service.UploadBatchService;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchPersistenceServiceImpl implements BatchPersistenceService {

    private final EntityRepository entityRepository;
    private final MasterAgreementRepository masterAgreementRepository;
    private final PaymentPolicyCatRepository paymentPolicyRepository;
    private final DisbursementPolicyCatRepository disbursementPolicyRepository;
    private final CreditFacilityRepository creditFacilityRepository;
    private final AcceptanceAuditService acceptanceAuditService;
    private final UploadBatchService uploadBatchService;
    private final DocumentRepository documentRepository;
    private final DocumentLogService documentLogService;
    private final EntityManager entityManager;
    private final EntityService entityService;
    private final DisbursementPolicyService disbursementPolicyService;
    private final BankAccountRepository bankAccountRepository;
    private final SystemParameters systemParameters;

    /**
     * Persiste la carga completa en una sola transacción: si se rechaza (límite de crédito,
     * duplicado concurrente), no quedan proveedores, cuentas ni convenios creados.
     * El cupo se bloquea al inicio para que las cargas de un mismo pagador no se intercalen.
     */
    @Override
    @Transactional
    public UUID persistBatch(
            EntityModel payer,
            Map<String, List<InvoiceRecordDTO>> invoicesBySupplier,
            String originalFilename,
            List<InvoiceRecordDTO> parsedInvoices,
            UUID uploadedAndApprovedBy,
            AcceptanceAuditDTORequest auditRequest) {

        CreditFacilityModel creditFacility = creditFacilityRepository.findByPayerIdForUpdate(payer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una línea de crédito activa asignada a este Pagador."));
        InactiveResourceException.requireActive(creditFacility.getStatus(),
                "La línea de crédito del pagador está inactiva; no se pueden cargar documentos.");

        Map<String, MasterAgreementModel> resolvedAgreements = new HashMap<>();
        invoicesBySupplier.forEach((supplierNit, supplierInvoices) ->
                resolvedAgreements.put(supplierNit, resolveOrCreateSupplierAndAgreement(payer, supplierInvoices.get(0))));

        return saveParsedBatchToDatabase(originalFilename, parsedInvoices, resolvedAgreements,
                uploadedAndApprovedBy, auditRequest, payer.getId());
    }

    private MasterAgreementModel resolveOrCreateSupplierAndAgreement(EntityModel payer, InvoiceRecordDTO recordInfo) {
        log.info("Verificando existencia del proveedor con NIT: {}", recordInfo.supplierNit());

        EntityModel supplier = entityRepository.findByNit(recordInfo.supplierNit()).orElse(null);

        if (supplier == null) {
            log.info("Proveedor no encontrado. Creando nuevo proveedor...");
            supplier = entityService.createSupplier(recordInfo.supplierNit(), recordInfo.supplierName());
        } else {
            InactiveResourceException.requireActive(supplier.getStatus(), String.format(
                    "El proveedor %s (NIT %s) está inactivo; no se pueden cargar sus documentos.",
                    supplier.getName(), supplier.getNit()));
        }

        ensureMainBankAccount(supplier, recordInfo.supplierAccountNumber());

        MasterAgreementModel agreement = masterAgreementRepository.findByPayerIdAndSupplierId(payer.getId(), supplier.getId()).orElse(null);

        if (agreement == null) {
            log.info("Convenio no encontrado. Creando nuevo convenio marco...");
            
            PaymentPolicyCat paymentPolicy = paymentPolicyRepository.findByCode(recordInfo.paymentPolicy())
                .orElseThrow(() -> new IllegalArgumentException("Política de pago no válida: " + recordInfo.paymentPolicy()));

            DisbursementPolicyCat disbursementPolicy = disbursementPolicyRepository.findByCode(recordInfo.disbursementDay())
                .orElseThrow(() -> new IllegalArgumentException("Política de desembolso no válida: " + recordInfo.disbursementDay()));
            disbursementPolicyService.validatePolicy(disbursementPolicy);

            agreement = MasterAgreementModel.builder()
                .payer(payer)
                .supplier(supplier)
                .paymentPolicy(paymentPolicy)
                .disbursementPolicy(disbursementPolicy)
                .agreementType(AgreementTypeEnum.STANDARD)
                .status(GeneralStatusEnum.ACTIVE)
                .build();

            agreement = masterAgreementRepository.save(agreement);
        } else {
            InactiveResourceException.requireActive(agreement.getStatus(), String.format(
                    "El convenio con el proveedor %s (NIT %s) está inactivo; no se pueden cargar sus documentos.",
                    supplier.getName(), supplier.getNit()));
        }

        return agreement;
    }

    /**
     * La coincidencia de la cuenta con la ya registrada se valida antes, en
     * DocumentValidationService; aquí solo se crea si el proveedor aún no tiene cuenta principal.
     */
    private void ensureMainBankAccount(EntityModel supplier, String accountNumber) {
        if (bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(supplier.getId()).isPresent()) {
            return;
        }

        log.info("Registrando cuenta bancaria principal para el proveedor {}", supplier.getId());
        bankAccountRepository.save(BankAccountModel.builder()
                .accountNumber(accountNumber.trim())
                .entityModel(supplier)
                .isMain(true)
                .status(GeneralStatusEnum.ACTIVE)
                .build());
    }

    /**
     * Guarda el lote y los documentos validados evaluando el crédito GLOBAL del pagador.
     */
    private UUID saveParsedBatchToDatabase(
            String originalFilename,
            List<InvoiceRecordDTO> parsedInvoices,
            Map<String, MasterAgreementModel> resolvedAgreements,
            UUID uploadedAndApprovedBy,
            AcceptanceAuditDTORequest auditRequest,
            UUID payerId) { 
            
        log.info("Phase II-A: Creando auditoría y lote principal");

        AcceptanceAuditDTOResponse savedAudit = acceptanceAuditService.createAcceptanceAudit(auditRequest);
        UUID acceptanceAuditId = savedAudit.id();

        String batchNumber = BatchNumberGeneratorUtil.generateBatchNumber(BatchTypeEnum.UPLOAD, disbursementPolicyService.businessToday());
        String fileName = FileNamingUtil.generateBatchFileName(batchNumber, originalFilename);

        UploadBatchDTORequest uploadBatchDTORequest = new UploadBatchDTORequest(
                batchNumber, fileName, parsedInvoices.size(), UploadBatchStatusEnum.COMPLETED, uploadedAndApprovedBy);

        UploadBatchDTOResponse savedUploadBatch = uploadBatchService.createUploadBatch(uploadBatchDTORequest);
        UploadBatchModel batchProxy = entityManager.getReference(UploadBatchModel.class, savedUploadBatch.id());

        log.info("Phase II-B: Validando Facilidad de Crédito GLOBAL a nivel de Pagador");

        BigDecimal totalBatchAmount = parsedInvoices.stream()
                .map(InvoiceRecordDTO::nominalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CreditFacilityModel globalCreditFacility = creditFacilityRepository
                .findByPayerIdForUpdate(payerId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró una línea de crédito activa asignada a este Pagador."));

        BigDecimal thresholdLimit = globalCreditFacility.uploadLimitAmount(
                systemParameters.getDecimal(SystemParameterKey.DEFAULT_CREDIT_THRESHOLD));
        BigDecimal currentUsedAmount = globalCreditFacility.getAmountInUse() != null
                ? globalCreditFacility.getAmountInUse() : BigDecimal.ZERO;

        BigDecimal newTotalUsed = currentUsedAmount.add(totalBatchAmount);

        if (newTotalUsed.compareTo(thresholdLimit) > 0) {
            BigDecimal availableThresholdAmount = thresholdLimit.subtract(currentUsedAmount);
            log.warn("Límite superado para el Pagador {}: Nuevo ({}) vs Límite ({})", payerId, newTotalUsed, thresholdLimit);
            throw new CreditLimitExceededException(totalBatchAmount, availableThresholdAmount.max(BigDecimal.ZERO));
        }

        globalCreditFacility.setAmountInUse(newTotalUsed);
        creditFacilityRepository.save(globalCreditFacility);

        log.info("Phase II-C: Iniciando guardado masivo en base de datos.");

        // entityManager.clear() desconecta los convenios: su política de pago (LAZY) ya no
        // podría inicializarse dentro del ciclo.
        Map<String, Integer> paymentDaysBySupplier = new HashMap<>();
        resolvedAgreements.forEach((supplierNit, agreement) ->
                paymentDaysBySupplier.put(supplierNit, agreement.getPaymentPolicy().getDaysCount()));

        final int CHUNK_SIZE = 1000;
        List<DocumentModel> batchToSave = new ArrayList<>(CHUNK_SIZE);

        for (int i = 0; i < parsedInvoices.size(); i++) {
            InvoiceRecordDTO dto = parsedInvoices.get(i);
            
            MasterAgreementModel masterAgreement = resolvedAgreements.get(dto.supplierNit());
            Integer paymentDays = paymentDaysBySupplier.get(dto.supplierNit());

            DocumentModel document = DocumentModel.builder()
                    .documentNumber(dto.documentNumber())
                    .issueDate(dto.issueDate())
                    .nominalAmount(dto.nominalAmount())
                    .generationCode(dto.generationCode())
                    .receivedStamp(dto.receivedStamp())
                    .controlNumber(dto.controlNumber())
                    .invoiceType(InvoiceTypeEnum.fromString(dto.invoiceType()))
                    .issuanceMethod(IssuanceMethodEnum.fromString(dto.issuanceMethod()))
                    .status(DocumentStatusEnum.APPROVED)
                    .masterAgreement(masterAgreement)
                    .uploadBatch(batchProxy)
                    .dueDate(dto.issueDate().plusDays(paymentDays != null ? paymentDays : 0))
                    .build();

            batchToSave.add(document);

            if ((i + 1) % CHUNK_SIZE == 0 || i == parsedInvoices.size() - 1) {
                documentRepository.saveAll(batchToSave);
                documentRepository.flush();

                documentLogService.createLogsForBatch(
                        batchToSave, DocumentStatusEnum.APPROVED, uploadedAndApprovedBy, acceptanceAuditId);

                entityManager.clear();
                batchToSave.clear();
            }
        }

        log.info("Lote {} multi-proveedor procesado y descontado del cupo global con éxito.", batchNumber);
        return savedUploadBatch.id();
    }
}