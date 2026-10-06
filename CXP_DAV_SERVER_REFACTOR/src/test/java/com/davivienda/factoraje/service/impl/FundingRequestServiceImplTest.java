package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
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
    private FundingCalculator fundingCalculator;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private MailNoticePublisher mailNotices;

    @InjectMocks
    private FundingRequestServiceImpl service;

    @Test
    void submitIsRejectedWhenSupplierHasNoMainBankAccount() {
        UUID agreementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EntityModel supplier = EntityModel.builder().id(UUID.randomUUID()).name("Proveedor Uno").build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(UserModel.builder().id(userId).build()));
        when(masterAgreementRepository.findById(agreementId))
                .thenReturn(Optional.of(MasterAgreementModel.builder().id(agreementId).supplier(supplier).build()));
        when(bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(supplier.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitFundingRequest(
                agreementId, userId, List.of(UUID.randomUUID()), UUID.randomUUID(), "JUnit"))
                .isInstanceOf(MissingBankAccountException.class)
                .hasMessageContaining("cuenta de abono");

        verify(documentRepository, never()).findAllByIdForUpdate(anyList());
        verify(financingRequestRepository, never()).save(any());
        verifyNoInteractions(mailNotices);
    }
}
