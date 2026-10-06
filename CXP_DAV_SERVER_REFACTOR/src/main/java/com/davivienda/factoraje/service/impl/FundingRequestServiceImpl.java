package com.davivienda.factoraje.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.entities.AcceptanceAuditModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DocumentLogModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.FinancingRequestModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.FinancingRequestStatusEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.domain.enums.TermTypeUniqueCodeEnum;
import com.davivienda.factoraje.dto.funding_request.CalculateDTOResponse;
import com.davivienda.factoraje.dto.funding_request.DetailDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.DocumentNotAvailableException;
import com.davivienda.factoraje.infrastructure.exception.InactiveResourceException;
import com.davivienda.factoraje.infrastructure.exception.MissingBankAccountException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.repository.AcceptanceAuditRepository;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.FinancingRequestRepository;
import com.davivienda.factoraje.repository.FinancingTransactionRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.repository.ProductPricingTermRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.FundingRequestService;
import com.davivienda.factoraje.service.TermVersionService;
import com.davivienda.factoraje.service.impl.FundingCalculator.CalculationResult;
import com.davivienda.factoraje.service.impl.FundingCalculator.DocumentMathResult;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FundingRequestServiceImpl implements FundingRequestService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MasterAgreementRepository masterAgreementRepository;
    private final CreditFacilityRepository creditFacilityRepository;
    private final ProductPricingTermRepository productPricingTermRepository;
    private final FinancingTransactionRepository financingTransactionRepository;
    private final TermVersionService termVersionService;
    private final FinancingRequestRepository financingRequestRepository;
    private final AcceptanceAuditRepository acceptanceAuditRepository;
    private final DocumentLogRepository documentLogRepository;
    private final SystemParameters systemParameters;
    private final DisbursementPolicyService disbursementPolicyService;
    private final FundingCalculator fundingCalculator;
    private final BankAccountRepository bankAccountRepository;
    private final MailNoticePublisher mailNotices;

    /** Carga y valida el convenio y su tarifario, y calcula; compartido por checkCost y submit. */
    private CalculationResult performFinancialCalculation(UUID masterAgreementId, List<DocumentModel> documents) {

        MasterAgreementModel masterAgreement = masterAgreementRepository.findById(masterAgreementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el convenio marco con ID: " + masterAgreementId));

        boolean foreignDocument = documents.stream()
                .anyMatch(doc -> !masterAgreementId.equals(doc.getMasterAgreement().getId()));
        if (foreignDocument) {
            throw new IllegalArgumentException("Uno o más documentos no pertenecen al convenio seleccionado.");
        }

        DisbursementPolicyCat disbursementPolicyCat = masterAgreement.getDisbursementPolicy();

        CreditFacilityModel creditFacility = creditFacilityRepository
                .findByPayerId(masterAgreement.getPayer().getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró la línea de crédito del pagador del convenio."));

        ProductPricingTermModel productPricing = productPricingTermRepository
                .findByCreditFacilityId(creditFacility.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el tarifario de la línea de crédito del convenio."));

        InactiveResourceException.requireActive(masterAgreement.getStatus(),
                "El convenio está inactivo; sus documentos no pueden financiarse.");
        InactiveResourceException.requireActive(masterAgreement.getSupplier().getStatus(),
                "El proveedor está inactivo; sus documentos no pueden financiarse.");
        InactiveResourceException.requireActive(masterAgreement.getPayer().getStatus(),
                "El pagador " + masterAgreement.getPayer().getName() + " está inactivo; sus documentos no pueden financiarse.");
        InactiveResourceException.requireActive(creditFacility.getStatus(),
                "La línea de crédito del pagador está inactiva; no se pueden financiar documentos.");
        InactiveResourceException.requireActive(productPricing.getStatus(),
                "El tarifario del pagador está inactivo; no se pueden financiar documentos.");

        FundingCalculator.Rates rates = new FundingCalculator.Rates(
                productPricing.getInterestRate(),
                productPricing.getCommissionRate(),
                productPricing.getCalculationBase().getDays(),
                systemParameters.getDecimal(SystemParameterKey.IVA_RATE));

        LocalDate disbursementDate = disbursementPolicyService.calculateDisbursementDateForNow(disbursementPolicyCat);

        log.debug("[CALCULO] Convenio {} | política de desembolso {} ({}) | hora de corte {} | fecha de desembolso {}",
                masterAgreementId,
                disbursementPolicyCat != null ? disbursementPolicyCat.getCode() : "-",
                disbursementPolicyCat != null ? disbursementPolicyCat.getType() : "-",
                disbursementPolicyService.cutoffTime(),
                DATE_FMT.format(disbursementDate));
        log.debug("[CALCULO] Tasa de interés {} | base {} días | tasa de comisión {} | IVA {} | {} documento(s)",
                rates.interestRate(), rates.baseDays(), rates.commissionRate(), rates.ivaRate(), documents.size());

        rejectNonFinanceable(documents, disbursementDate);

        return fundingCalculator.calculate(documents, rates, disbursementDate);
    }

    /**
     * Rechaza la operación si algún documento vence dentro del período de gracia
     * posterior a la fecha de desembolso (riesgo de usura). Es la misma regla con
     * la que se filtra la lista de documentos financiables.
     */
    private void rejectNonFinanceable(List<DocumentModel> documents, LocalDate disbursementDate) {
        int graceDays = disbursementPolicyService.dueDateGraceDays();
        LocalDate threshold = disbursementDate.plusDays(graceDays);

        List<String> rejected = documents.stream()
                .filter(doc -> doc.getDueDate() == null || !doc.getDueDate().isAfter(threshold))
                .map(doc -> doc.getDocumentNumber() != null ? doc.getDocumentNumber() : doc.getControlNumber())
                .toList();

        if (!rejected.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "Los siguientes documentos ya no pueden financiarse porque vencen antes del %s "
                            + "(fecha de desembolso %s + %d días): %s",
                    DATE_FMT.format(threshold.plusDays(1)),
                    DATE_FMT.format(disbursementDate),
                    graceDays,
                    String.join(", ", rejected)));
        }
    }

    @Transactional
    @Override
    public CalculateDTOResponse submitFundingRequest(
            UUID masterAgreementId,
            UUID requestedById,
            List<UUID> documentIds,
            UUID termVersionId,
            String userAgent) {

        if (documentIds == null || documentIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de documentos a financiar no puede estar vacía.");
        }
        requireDistinct(documentIds);

        UserModel requestedBy = userRepository.findById(requestedById)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el Usuario con ID: " + requestedById));
        MasterAgreementModel agreement = masterAgreementRepository.findById(masterAgreementId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el convenio marco con ID: " + masterAgreementId));
        EntityModel supplier = agreement.getSupplier();
        if (bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(supplier.getId()).isEmpty()) {
            throw new MissingBankAccountException(
                    "No tiene una cuenta de abono registrada. Comuníquese con el banco para registrarla antes de solicitar el desembolso.");
        }
        TermVersionCat termVersion = termVersionService.requireActiveVersion(
                termVersionId, TermTypeUniqueCodeEnum.SUPPLIER_TERM_TYPE);

        // El bloqueo hace que una solicitud simultánea sobre los mismos documentos espere y luego
        // los encuentre ya solicitados, en vez de crear una segunda transacción de financiamiento.
        List<DocumentModel> realDocuments = documentRepository.findAllByIdForUpdate(documentIds);
        if (realDocuments.size() != documentIds.size()) {
            throw new ResourceNotFoundException("Uno o más documentos solicitados no existen en la BD.");
        }

        for (DocumentModel doc : realDocuments) {
            if (doc.getStatus() != DocumentStatusEnum.APPROVED) {
                log.warn("Intento de fondeo duplicado. El documento {} está en estado {}.",
                        doc.getDocumentNumber(), doc.getStatus());

                throw new DocumentNotAvailableException("El documento ya fue solicitado o no está disponible para financiamiento.");
            }
        }

        CalculationResult result = performFinancialCalculation(masterAgreementId, realDocuments);

        FinancingRequestModel requestModel = new FinancingRequestModel();
        requestModel.setRequestNumber("REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        requestModel.setTotalAmountToFinance(result.totalAmountToFinance());
        requestModel.setTotalAmountToBeDisbursed(result.totalAmountToDisburse());
        requestModel.setTotalFlatAmount(result.totalAmount());
        requestModel.setStatus(FinancingRequestStatusEnum.SUBMITTED);
        requestModel.setSupplier(supplier);
        requestModel.setRequestedBy(requestedBy);

        financingRequestRepository.save(requestModel);

        // Se guarda antes de los logs de documento, que lo referencian.
        AcceptanceAuditModel audit = new AcceptanceAuditModel();
        audit.setUserAgent(userAgent);
        audit.setUser(requestedBy);
        audit.setTermVersion(termVersion);

        acceptanceAuditRepository.save(audit);

        List<FinancingTransactionModel> transactions = new ArrayList<>();
        List<DocumentLogModel> documentLogs = new ArrayList<>();

        for (DocumentMathResult docMath : result.details()) {
            DocumentModel document = docMath.document();

            FinancingTransactionModel transaction = new FinancingTransactionModel();
            transaction.setFinancingRequest(requestModel);
            transaction.setDocument(document);
            transaction.setInterestAmount(docMath.interest());
            transaction.setCommissionAmount(docMath.commission());
            transaction.setAmountToFinance(docMath.amountToFinance());
            transaction.setFlatAmount(docMath.nominalAmount());
            transaction.setAmountToBeDisbursed(docMath.disburse());
            transaction.setDiscountRate(docMath.discountRate());
            transaction.setIvaAmount(docMath.ivaAmount());
            transaction.setFinancingPercentage(docMath.financingPercentage());
            transaction.setScheduledDisbursementDate(result.disbursementDate());

            transactions.add(transaction);

            document.setStatus(DocumentStatusEnum.REQUESTED_FOR_FINANCING);

            DocumentLogModel documentLog = new DocumentLogModel();
            documentLog.setDocument(document);
            documentLog.setUser(requestedBy);
            documentLog.setStatus(DocumentStatusEnum.REQUESTED_FOR_FINANCING);
            documentLog.setAcceptanceAudit(audit);

            documentLogs.add(documentLog);
        }

        financingTransactionRepository.saveAll(transactions);
        documentRepository.saveAll(realDocuments);
        documentLogRepository.saveAll(documentLogs);

        mailNotices.fundingRequested(requestModel.getRequestNumber(), supplier.getName(),
                agreement.getPayer() != null ? agreement.getPayer().getName() : null, requestedBy.fullName(),
                realDocuments.size(), result.totalAmount(), result.totalAmountToDisburse(), result.disbursementDate());
        return toResponse(result);
    }

    private static void requireDistinct(List<UUID> documentIds) {
        if (new HashSet<>(documentIds).size() != documentIds.size()) {
            throw new IllegalArgumentException("La lista contiene documentos repetidos.");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public CalculateDTOResponse checkCost(UUID masterAgreementId, List<UUID> documentIds) {

        if (documentIds == null || documentIds.isEmpty()) {
            throw new IllegalArgumentException("La lista de documentos no puede ser nula o estar vacía");
        }

        log.info("[SIMULACION] Iniciando simulación de cálculo para {} documento(s) del convenio {}.",
                documentIds.size(), masterAgreementId);

        requireDistinct(documentIds);
        Map<UUID, DocumentModel> documentsById = documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(DocumentModel::getId, Function.identity()));
        if (documentsById.size() != documentIds.size()) {
            throw new ResourceNotFoundException("Uno o más documentos solicitados no existen.");
        }
        List<DocumentModel> realDocuments = documentIds.stream().map(documentsById::get).toList();

        return toResponse(performFinancialCalculation(masterAgreementId, realDocuments));
    }

    private static CalculateDTOResponse toResponse(CalculationResult result) {
        CalculateDTOResponse response = new CalculateDTOResponse();

        for (DocumentMathResult docMath : result.details()) {
            DetailDTOResponse detail = new DetailDTOResponse();
            detail.setDocumentNumber(docMath.document().getDocumentNumber());
            detail.setIssueDate(docMath.issueDate());
            detail.setDueDate(docMath.dueDate());
            detail.setFinancingDays(docMath.diffDays());
            detail.setAmount(Money.round(docMath.nominalAmount()));
            detail.setAmountToFinance(Money.round(docMath.amountToFinance()));
            detail.setInterests(Money.round(docMath.interest()));
            detail.setCommissions(Money.round(docMath.commissionWithIva()));
            detail.setAmountToBeDisbursed(Money.round(docMath.disburse()));
            response.getDetail().add(detail);
        }

        response.setAmountToFinance(Money.round(result.totalAmountToFinance()));
        response.setAmount(Money.round(result.totalAmount()));
        response.setInterests(Money.round(result.totalInterests()));
        response.setCommissions(Money.round(result.totalCommissions()));
        response.setAmountToBeDisbursed(Money.round(result.totalAmountToDisburse()));
        response.setDisbursementDate(result.disbursementDate());

        return response;
    }
}
