package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DocumentLogModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.QuarantineReasonEnum;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.CreditFacilityHistoryRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplInactivationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private MasterAgreementRepository masterAgreementRepository;
    @Mock
    private DisbursementPolicyService disbursementPolicyService;
    @Mock
    private DocumentLogRepository documentLogRepository;
    @Mock
    private CreditFacilityRepository creditFacilityRepository;
    @Mock
    private CreditFacilityHistoryRepository creditFacilityHistoryRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DocumentServiceImpl service;

    private final DisbursementPolicyCat policy = DisbursementPolicyCat.builder().code("T_PLUS_1").build();
    private EntityModel payer;
    private MasterAgreementModel agreement;

    @BeforeEach
    void setUp() {
        payer = EntityModel.builder().id(UUID.randomUUID()).name("Pagador").build();
        agreement = MasterAgreementModel.builder()
                .id(UUID.randomUUID())
                .payer(payer)
                .disbursementPolicy(policy)
                .build();
        lenient().when(disbursementPolicyService.businessToday()).thenReturn(TODAY);
        lenient().when(creditFacilityRepository.findByPayerIdForUpdate(payer.getId())).thenReturn(Optional.of(
                CreditFacilityModel.builder().payer(payer).amountInUse(new BigDecimal("5000.00")).build()));
    }

    @Test
    void payerInactivationLeavesTheDocumentInactivatedByPayer() {
        UserModel payerUser = UserModel.builder().id(UUID.randomUUID()).entity(payer).build();
        DocumentModel document = document(TODAY.plusDays(3));
        when(currentUserService.managed()).thenReturn(payerUser);
        when(documentRepository.findAllByIdForUpdate(Set.of(document.getId()))).thenReturn(List.of(document));

        service.inactivateDocumentByPayer(document.getId());

        assertThat(document.getStatus()).isEqualTo(DocumentStatusEnum.INACTIVATED_BY_PAYER);
        assertThat(document.getQuarantineReason()).isNull();
        assertThat(document.getQuarantinedAt()).isNotNull();
        assertThat(savedLogStatuses()).containsExactly(DocumentStatusEnum.INACTIVATED_BY_PAYER);
    }

    @Test
    void payerCanInactivateAStillFinanceableDocument() {
        UserModel payerUser = UserModel.builder().id(UUID.randomUUID()).entity(payer).build();
        DocumentModel document = document(TODAY.plusDays(40));
        when(currentUserService.managed()).thenReturn(payerUser);
        when(documentRepository.findAllByIdForUpdate(Set.of(document.getId()))).thenReturn(List.of(document));

        service.inactivateDocumentByPayer(document.getId());

        assertThat(document.getStatus()).isEqualTo(DocumentStatusEnum.INACTIVATED_BY_PAYER);
        assertThat(document.getQuarantineReason()).isNull();
        assertThat(savedLogStatuses()).containsExactly(DocumentStatusEnum.INACTIVATED_BY_PAYER);
    }

    @Test
    void automaticInactivationLeavesTheDocumentInQuarantine() {
        UserModel supplierUser = UserModel.builder().id(UUID.randomUUID()).build();
        DocumentModel expired = document(TODAY.minusDays(1));
        LocalDate threshold = TODAY.plusDays(6);
        when(currentUserService.managed()).thenReturn(supplierUser);
        when(masterAgreementRepository.findById(agreement.getId())).thenReturn(Optional.of(agreement));
        when(disbursementPolicyService.financeableDueDateThreshold(policy)).thenReturn(threshold);
        when(documentRepository.findByMasterAgreementAndStatusAndDueDateLessThanEqualForUpdate(
                agreement.getId(), DocumentStatusEnum.APPROVED, threshold)).thenReturn(List.of(expired));
        when(documentRepository.findByMasterAgreementAndStatusAndDueDateAfter(
                eq(agreement.getId()), eq(DocumentStatusEnum.APPROVED), any())).thenReturn(List.of());

        service.getFinanceableDocumentsByMasterAgreement(agreement.getId());

        assertThat(expired.getStatus()).isEqualTo(DocumentStatusEnum.IN_QUARANTINE);
        assertThat(expired.getQuarantineReason()).isEqualTo(QuarantineReasonEnum.DUE_DATE_EXPIRED);
        assertThat(savedLogStatuses()).containsExactly(DocumentStatusEnum.IN_QUARANTINE);
    }

    @Test
    void payerCannotInactivateADocumentThatIsNoLongerLoaded() {
        UserModel payerUser = UserModel.builder().id(UUID.randomUUID()).entity(payer).build();
        DocumentModel document = document(TODAY.plusDays(40));
        document.setStatus(DocumentStatusEnum.REQUESTED_FOR_FINANCING);
        when(currentUserService.managed()).thenReturn(payerUser);
        when(documentRepository.findAllByIdForUpdate(Set.of(document.getId()))).thenReturn(List.of(document));

        assertThatThrownBy(() -> service.inactivateDocumentByPayer(document.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cargado");
        assertThat(document.getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED_FOR_FINANCING);
        verify(documentLogRepository, never()).saveAll(anyList());
    }

    @Test
    void payerInactivatesSeveralDocumentsAtOnceAndReleasesTheirCredit() {
        UserModel payerUser = UserModel.builder().id(UUID.randomUUID()).entity(payer).build();
        DocumentModel first = document(TODAY.plusDays(40));
        DocumentModel second = document(TODAY.plusDays(3));
        CreditFacilityModel facility = CreditFacilityModel.builder()
                .payer(payer).amountInUse(new BigDecimal("5000.00")).build();
        when(currentUserService.managed()).thenReturn(payerUser);
        when(documentRepository.findAllByIdForUpdate(Set.of(first.getId(), second.getId())))
                .thenReturn(List.of(first, second));
        when(creditFacilityRepository.findByPayerIdForUpdate(payer.getId())).thenReturn(Optional.of(facility));

        service.inactivateDocumentsByPayer(List.of(first.getId(), second.getId(), first.getId()));

        assertThat(first.getStatus()).isEqualTo(DocumentStatusEnum.INACTIVATED_BY_PAYER);
        assertThat(second.getStatus()).isEqualTo(DocumentStatusEnum.INACTIVATED_BY_PAYER);
        assertThat(savedLogStatuses()).containsExactly(
                DocumentStatusEnum.INACTIVATED_BY_PAYER, DocumentStatusEnum.INACTIVATED_BY_PAYER);
        assertThat(facility.getAmountInUse()).isEqualByComparingTo("3000.00");
    }

    @Test
    void bulkInactivationIsAllOrNothingWhenOneDocumentIsNoLongerLoaded() {
        UserModel payerUser = UserModel.builder().id(UUID.randomUUID()).entity(payer).build();
        DocumentModel loaded = document(TODAY.plusDays(40));
        DocumentModel requested = document(TODAY.plusDays(30));
        requested.setStatus(DocumentStatusEnum.REQUESTED_FOR_FINANCING);
        when(currentUserService.managed()).thenReturn(payerUser);
        when(documentRepository.findAllByIdForUpdate(Set.of(loaded.getId(), requested.getId())))
                .thenReturn(List.of(loaded, requested));

        assertThatThrownBy(() -> service.inactivateDocumentsByPayer(List.of(loaded.getId(), requested.getId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(requested.getDocumentNumber());
        assertThat(loaded.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
        verify(documentLogRepository, never()).saveAll(anyList());
    }

    @Test
    void bulkInactivationRejectsDocumentsOfAnotherPayer() {
        EntityModel otherPayer = EntityModel.builder().id(UUID.randomUUID()).build();
        UserModel otherPayerUser = UserModel.builder().id(UUID.randomUUID()).entity(otherPayer).build();
        DocumentModel document = document(TODAY.plusDays(40));
        when(currentUserService.managed()).thenReturn(otherPayerUser);
        when(documentRepository.findAllByIdForUpdate(Set.of(document.getId()))).thenReturn(List.of(document));

        assertThatThrownBy(() -> service.inactivateDocumentsByPayer(List.of(document.getId())))
                .isInstanceOf(UnauthorizedAccessException.class);
        assertThat(document.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
    }

    @Test
    void bulkInactivationReportsMissingDocuments() {
        UserModel payerUser = UserModel.builder().id(UUID.randomUUID()).entity(payer).build();
        DocumentModel document = document(TODAY.plusDays(40));
        UUID missing = UUID.randomUUID();
        when(currentUserService.managed()).thenReturn(payerUser);
        when(documentRepository.findAllByIdForUpdate(Set.of(document.getId(), missing))).thenReturn(List.of(document));

        assertThatThrownBy(() -> service.inactivateDocumentsByPayer(List.of(document.getId(), missing)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(missing.toString());
        assertThat(document.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
    }

    private DocumentModel document(LocalDate dueDate) {
        return DocumentModel.builder()
                .id(UUID.randomUUID())
                .documentNumber("DOC-" + dueDate)
                .dueDate(dueDate)
                .nominalAmount(new BigDecimal("1000.00"))
                .status(DocumentStatusEnum.APPROVED)
                .masterAgreement(agreement)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<DocumentStatusEnum> savedLogStatuses() {
        ArgumentCaptor<List<DocumentLogModel>> logs = ArgumentCaptor.forClass(List.class);
        verify(documentLogRepository).saveAll(logs.capture());
        return logs.getValue().stream().map(DocumentLogModel::getStatus).toList();
    }
}
