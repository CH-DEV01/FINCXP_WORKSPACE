package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.funding_request.CalculateDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.FundingQuoteChangedException;
import com.davivienda.factoraje.infrastructure.exception.MissingBankAccountException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
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
import com.davivienda.factoraje.service.TermVersionService;

@ExtendWith(MockitoExtension.class)
class FundingRequestServiceImplTest {

    private static final LocalDate QUOTED_DATE = LocalDate.of(2026, 10, 15);

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MasterAgreementRepository masterAgreementRepository;
    @Mock
    private CreditFacilityRepository creditFacilityRepository;
    @Mock
    private ProductPricingTermRepository productPricingTermRepository;
    @Mock
    private FinancingTransactionRepository financingTransactionRepository;
    @Mock
    private TermVersionService termVersionService;
    @Mock
    private FinancingRequestRepository financingRequestRepository;
    @Mock
    private AcceptanceAuditRepository acceptanceAuditRepository;
    @Mock
    private DocumentLogRepository documentLogRepository;
    @Mock
    private SystemParameters systemParameters;
    @Mock
    private DisbursementPolicyService disbursementPolicyService;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private MailNoticePublisher mailNotices;

    // Calculadora real: la cotización comparada es la que de verdad produciría el sistema.
    @Spy
    private FundingCalculator fundingCalculator = new FundingCalculator();

    @InjectMocks
    private FundingRequestServiceImpl service;

    private final UUID agreementId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID termVersionId = UUID.randomUUID();
    private final EntityModel supplier = EntityModel.builder()
            .id(UUID.randomUUID()).name("Proveedor Uno").status(GeneralStatusEnum.ACTIVE).build();
    private final EntityModel payer = EntityModel.builder()
            .id(UUID.randomUUID()).name("Pagador Uno").status(GeneralStatusEnum.ACTIVE).build();

    @Test
    void submitIsRejectedWhenSupplierHasNoMainBankAccount() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(UserModel.builder().id(userId).build()));
        when(masterAgreementRepository.findById(agreementId))
                .thenReturn(Optional.of(MasterAgreementModel.builder().id(agreementId).supplier(supplier).build()));
        when(bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(supplier.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitFundingRequest(
                agreementId, userId, List.of(UUID.randomUUID()), termVersionId,
                QUOTED_DATE, new BigDecimal("986.98"), "JUnit"))
                .isInstanceOf(MissingBankAccountException.class)
                .hasMessageContaining("cuenta de abono");

        verify(documentRepository, never()).findAllByIdForUpdate(anyList());
        verify(financingRequestRepository, never()).save(any());
        verifyNoInteractions(mailNotices);
    }

    @Test
    void submitIsRejectedWhenTheDisbursementDateMovedPastTheCutoff() {
        DocumentModel document = readyToSubmit(QUOTED_DATE.plusDays(1));

        assertThatThrownBy(() -> service.submitFundingRequest(
                agreementId, userId, List.of(document.getId()), termVersionId,
                QUOTED_DATE, new BigDecimal("986.98"), "JUnit"))
                .isInstanceOf(FundingQuoteChangedException.class)
                .hasMessageContaining("fecha de desembolso pasó del 15/10/2026 al 16/10/2026");

        verify(financingRequestRepository, never()).save(any());
        verify(financingTransactionRepository, never()).saveAll(any());
        assertThat(document.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
        verifyNoInteractions(mailNotices);
    }

    @Test
    void submitIsRejectedWhenTheAmountToDisburseDiffersFromTheAcceptedQuote() {
        DocumentModel document = readyToSubmit(QUOTED_DATE);

        assertThatThrownBy(() -> service.submitFundingRequest(
                agreementId, userId, List.of(document.getId()), termVersionId,
                QUOTED_DATE, new BigDecimal("990.00"), "JUnit"))
                .isInstanceOf(FundingQuoteChangedException.class)
                .hasMessageContaining("monto a desembolsar pasó de $990.00 a $986.98");

        verify(financingRequestRepository, never()).save(any());
        verifyNoInteractions(mailNotices);
    }

    @Test
    void submitSucceedsWhenDateAndAmountMatchTheAcceptedQuote() {
        DocumentModel document = readyToSubmit(QUOTED_DATE);

        CalculateDTOResponse response = service.submitFundingRequest(
                agreementId, userId, List.of(document.getId()), termVersionId,
                QUOTED_DATE, new BigDecimal("986.98"), "JUnit");

        assertThat(response.getDisbursementDate()).isEqualTo(QUOTED_DATE);
        assertThat(response.getAmountToBeDisbursed()).isEqualByComparingTo("986.98");
        assertThat(document.getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED_FOR_FINANCING);
        verify(financingRequestRepository).save(any());
    }

    /**
     * Un documento APPROVED de 1 000.00 que vence 31 días después del 15/10/2026, con las
     * tasas del test del calculador: cotiza 986.98 si se desembolsa el 15/10.
     */
    private DocumentModel readyToSubmit(LocalDate disbursementDateNow) {
        DisbursementPolicyCat policy = DisbursementPolicyCat.builder().id(UUID.randomUUID()).code("T0").build();
        MasterAgreementModel agreement = MasterAgreementModel.builder()
                .id(agreementId).supplier(supplier).payer(payer)
                .status(GeneralStatusEnum.ACTIVE).disbursementPolicy(policy).build();
        CreditFacilityModel facility = CreditFacilityModel.builder()
                .id(UUID.randomUUID()).payer(payer).status(GeneralStatusEnum.ACTIVE).build();
        ProductPricingTermModel pricing = ProductPricingTermModel.builder()
                .id(UUID.randomUUID())
                .interestRate(new BigDecimal("0.12"))
                .commissionRate(new BigDecimal("0.0025"))
                .calculationBase(CalculationBaseEnum.COMERCIAL_360)
                .status(GeneralStatusEnum.ACTIVE)
                .build();
        DocumentModel document = DocumentModel.builder()
                .id(UUID.randomUUID())
                .documentNumber("F-001")
                .masterAgreement(agreement)
                .status(DocumentStatusEnum.APPROVED)
                .issueDate(QUOTED_DATE.minusDays(10))
                .dueDate(QUOTED_DATE.plusDays(30))
                .nominalAmount(new BigDecimal("1000.00"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(UserModel.builder().id(userId).build()));
        when(masterAgreementRepository.findById(agreementId)).thenReturn(Optional.of(agreement));
        when(bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(supplier.getId()))
                .thenReturn(Optional.of(new BankAccountModel()));
        when(termVersionService.requireActiveVersion(eq(termVersionId), any()))
                .thenReturn(TermVersionCat.builder().id(termVersionId).build());
        when(documentRepository.findAllByIdForUpdate(List.of(document.getId()))).thenReturn(List.of(document));
        when(creditFacilityRepository.findByPayerId(payer.getId())).thenReturn(Optional.of(facility));
        when(productPricingTermRepository.findByCreditFacilityId(facility.getId())).thenReturn(Optional.of(pricing));
        when(systemParameters.getDecimal(any())).thenReturn(new BigDecimal("0.13"));
        when(disbursementPolicyService.calculateDisbursementDateForNow(policy)).thenReturn(disbursementDateNow);
        when(disbursementPolicyService.dueDateGraceDays()).thenReturn(0);
        lenient().when(disbursementPolicyService.cutoffTime()).thenReturn(null);
        return document;
    }
}
