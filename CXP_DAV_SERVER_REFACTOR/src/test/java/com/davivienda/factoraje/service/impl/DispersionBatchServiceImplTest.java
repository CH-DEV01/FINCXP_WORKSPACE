package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.DispersionBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DispersionBatchStatusEnum;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchSummaryDTOResponse;
import com.davivienda.factoraje.dto.report.DispersionLetterData;
import com.davivienda.factoraje.infrastructure.exception.DocumentNotAvailableException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.repository.DispersionBatchRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.DispersionBatchService.Letter;
import com.davivienda.factoraje.service.DispersionLetterPdfService;
import com.davivienda.factoraje.service.DocumentService;

class DispersionBatchServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final LocalDate DUE_DATE = TODAY.plusDays(3);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EntityRepository entityRepository = mock(EntityRepository.class);
    private final BankAccountRepository bankAccountRepository = mock(BankAccountRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentLogRepository documentLogRepository = mock(DocumentLogRepository.class);
    private final DispersionBatchRepository dispersionBatchRepository = mock(DispersionBatchRepository.class);
    private final DisbursementPolicyCatRepository policyRepository = mock(DisbursementPolicyCatRepository.class);
    private final DisbursementPolicyService policyService = mock(DisbursementPolicyService.class);
    private final DocumentService documentService = mock(DocumentService.class);
    private final DispersionLetterPdfService letterPdfService = mock(DispersionLetterPdfService.class);
    private final MailNoticePublisher mailNotices = mock(MailNoticePublisher.class);

    private DispersionBatchServiceImpl service;

    // T+1 deja de financiar lo que vence en 6 días; la otra política aún financia lo que vence hoy + 3.
    private final DisbursementPolicyCat tPlusOne = DisbursementPolicyCat.builder().code("T_PLUS_1").build();
    private final DisbursementPolicyCat lenientPolicy = DisbursementPolicyCat.builder().code("LENIENT").build();

    private final UserModel admin = UserModel.builder().id(UUID.randomUUID()).firstName("Admin").build();
    private final UserModel olderUploader = UserModel.builder().id(UUID.randomUUID()).firstName("Ana").build();
    private final UserModel latestUploader = UserModel.builder()
            .id(UUID.randomUUID()).firstName("Luis").lastName("Pérez").dui("000000000").build();
    private final EntityModel payer = EntityModel.builder().id(UUID.randomUUID()).name("Pagador Demo").build();
    private final EntityModel supplier = EntityModel.builder().id(UUID.randomUUID()).name("Proveedor Uno").build();

    @BeforeEach
    void setUp() {
        DispersionDocuments dispersionDocuments = new DispersionDocuments(
                documentRepository, policyRepository, bankAccountRepository, policyService);
        service = new DispersionBatchServiceImpl(userRepository, entityRepository, bankAccountRepository,
                documentRepository, documentLogRepository, dispersionBatchRepository, policyService, documentService,
                dispersionDocuments, new DispersionLetterAssembler(dispersionDocuments), letterPdfService,
                mailNotices);

        lenient().when(policyService.businessToday()).thenReturn(TODAY);
        lenient().when(policyService.financeableDueDateThreshold(tPlusOne)).thenReturn(TODAY.plusDays(6));
        lenient().when(policyService.financeableDueDateThreshold(lenientPolicy)).thenReturn(TODAY);
        lenient().when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        lenient().when(entityRepository.findByIdForUpdate(payer.getId())).thenReturn(Optional.of(payer));
        lenient().when(bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(payer.getId()))
                .thenReturn(Optional.of(account(payer, "1000200030")));
        lenient().when(bankAccountRepository.findAllByEntityModelIdInAndIsMainTrue(anyCollection()))
                .thenReturn(List.of(account(supplier, "2345678992")));
        lenient().when(dispersionBatchRepository.save(any(DispersionBatchModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(letterPdfService.generate(any())).thenReturn(new byte[] {1});
        lenient().doAnswer(invocation -> {
            List<DocumentModel> documents = invocation.getArgument(0);
            documents.forEach(doc -> doc.setStatus(DocumentStatusEnum.IN_QUARANTINE));
            return null;
        }).when(documentService).quarantineNonFinanceable(anyList(), any());
    }

    @Test
    void generatesTheBatchWithQuarantinedAndNoLongerFinanceableLoadedDocuments() {
        DocumentModel quarantined = document(DocumentStatusEnum.IN_QUARANTINE, tPlusOne, "5000.00", olderUploader, 1);
        DocumentModel expiredLoaded = document(DocumentStatusEnum.APPROVED, tPlusOne, "12347.00", latestUploader, 2);
        DocumentModel stillFinanceable = document(DocumentStatusEnum.APPROVED, lenientPolicy, "800.00", olderUploader, 3);
        when(documentRepository.findWithoutDispersionBatchForUpdate(eq(payer.getId()), eq(DUE_DATE), any()))
                .thenReturn(List.of(quarantined, expiredLoaded, stillFinanceable));

        Letter letter = service.generateBatch(admin.getId(), payer.getId(), DUE_DATE);

        verify(documentService).quarantineNonFinanceable(List.of(expiredLoaded), admin);

        ArgumentCaptor<DispersionBatchModel> batchCaptor = ArgumentCaptor.forClass(DispersionBatchModel.class);
        verify(dispersionBatchRepository).save(batchCaptor.capture());
        DispersionBatchModel batch = batchCaptor.getValue();
        assertThat(batch.getDocumentCount()).isEqualTo(2);
        assertThat(batch.getTotalAmount()).isEqualByComparingTo("17347.00");
        assertThat(batch.getPayerAccountNumber()).isEqualTo("1000200030");
        assertThat(batch.getDueDate()).isEqualTo(DUE_DATE);
        assertThat(batch.getDispersionDate()).isEqualTo(DUE_DATE);
        assertThat(batch.getStatus()).isEqualTo(DispersionBatchStatusEnum.CREATED);
        assertThat(batch.getSigner()).isSameAs(latestUploader);
        assertThat(batch.getBatchNumber()).startsWith("DSP-20261002-");
        verify(mailNotices).dispersionBatchCreated(batch.getBatchNumber(), "Pagador Demo", DUE_DATE, 2);
        verify(mailNotices, never()).operatorChanged(any(), any(), any());

        assertThat(List.of(quarantined, expiredLoaded)).allSatisfy(doc -> {
            assertThat(doc.getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED_FOR_DISPERSION);
            assertThat(doc.getDispersionBatch()).isSameAs(batch);
        });
        assertThat(stillFinanceable.getStatus()).isEqualTo(DocumentStatusEnum.APPROVED);
        assertThat(stillFinanceable.getDispersionBatch()).isNull();

        ArgumentCaptor<DispersionLetterData> letterCaptor = ArgumentCaptor.forClass(DispersionLetterData.class);
        verify(letterPdfService).generate(letterCaptor.capture());
        DispersionLetterData data = letterCaptor.getValue();
        assertThat(data.dispersionDate()).isEqualTo("5 de octubre de 2026");
        assertThat(data.signerName()).isEqualTo("Luis Pérez");
        assertThat(data.signerDui()).isEqualTo("000000000");
        assertThat(data.companyName()).isEqualTo("Pagador Demo");
        assertThat(data.payerAccountNumber()).isEqualTo("1000200030");
        assertThat(data.rows()).singleElement().satisfies(row -> {
            assertThat(row.dispersionDate()).isEqualTo("05/10/2026");
            assertThat(row.accountName()).isEqualTo("Proveedor Uno");
            assertThat(row.accountNumber()).isEqualTo("2345678992");
            assertThat(row.recordCount()).isEqualTo("2");
            assertThat(row.dueDate()).isEqualTo("05/10/2026");
            assertThat(row.invoiceAmount()).isEqualTo("$17,347.00");
        });
        assertThat(letter.fileName()).isEqualTo("Solicitud_Dispersion_" + batch.getBatchNumber() + ".pdf");
    }

    @Test
    void dispersesOnTheDueDateEvenWhenItIsAPastSunday() {
        LocalDate pastSunday = LocalDate.of(2026, 9, 20);
        DocumentModel quarantined = document(DocumentStatusEnum.IN_QUARANTINE, tPlusOne, "100.00", olderUploader, 1);
        quarantined.setDueDate(pastSunday);
        when(documentRepository.findWithoutDispersionBatchForUpdate(eq(payer.getId()), eq(pastSunday), any()))
                .thenReturn(List.of(quarantined));

        service.generateBatch(admin.getId(), payer.getId(), pastSunday);

        ArgumentCaptor<DispersionBatchModel> batchCaptor = ArgumentCaptor.forClass(DispersionBatchModel.class);
        verify(dispersionBatchRepository).save(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getDispersionDate()).isEqualTo(pastSunday);
        assertThat(quarantined.getStatus()).isEqualTo(DocumentStatusEnum.REQUESTED_FOR_DISPERSION);
    }

    @Test
    void rejectsAPayerWithoutMainAccount() {
        when(bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(payer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateBatch(admin.getId(), payer.getId(), DUE_DATE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene una cuenta principal");
        verify(dispersionBatchRepository, never()).save(any());
    }

    @Test
    void rejectsADueDateWithoutDocumentsToDisperse() {
        DocumentModel stillFinanceable = document(DocumentStatusEnum.APPROVED, lenientPolicy, "800.00", olderUploader, 1);
        when(documentRepository.findWithoutDispersionBatchForUpdate(eq(payer.getId()), eq(DUE_DATE), any()))
                .thenReturn(List.of(stillFinanceable));

        assertThatThrownBy(() -> service.generateBatch(admin.getId(), payer.getId(), DUE_DATE))
                .isInstanceOf(DocumentNotAvailableException.class);
        verify(documentService, never()).quarantineNonFinanceable(anyList(), any());
    }

    @Test
    void confirmingTheBatchLeavesItsDocumentsDispersed() {
        DispersionBatchModel batch = batch(DispersionBatchStatusEnum.CREATED);
        DocumentModel first = document(DocumentStatusEnum.REQUESTED_FOR_DISPERSION, tPlusOne, "100.00", olderUploader, 1);
        DocumentModel second = document(DocumentStatusEnum.REQUESTED_FOR_DISPERSION, tPlusOne, "200.00", olderUploader, 2);
        when(dispersionBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));
        when(documentRepository.findByDispersionBatchIdForUpdate(batch.getId())).thenReturn(List.of(first, second));

        DispersionBatchSummaryDTOResponse summary = service.confirmBatch(batch.getId(), admin.getId());

        assertThat(first.getStatus()).isEqualTo(DocumentStatusEnum.DISPERSED);
        assertThat(second.getStatus()).isEqualTo(DocumentStatusEnum.DISPERSED);
        assertThat(summary.status()).isEqualTo(DispersionBatchStatusEnum.SETTLED);
        assertThat(batch.getConfirmedBy()).isSameAs(admin);
        assertThat(batch.getConfirmedAt()).isNotNull();
        verify(mailNotices).operatorChanged("Dispersiones",
                "Lote " + batch.getBatchNumber() + ": Creado",
                "Lote " + batch.getBatchNumber() + ": Liquidado (2 documento(s) dispersados)");
        verify(mailNotices, never()).dispersionBatchCreated(any(), any(), any(), anyInt());
    }

    @Test
    void aConfirmedBatchCannotBeConfirmedAgain() {
        DispersionBatchModel batch = batch(DispersionBatchStatusEnum.SETTLED);
        when(dispersionBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> service.confirmBatch(batch.getId(), admin.getId()))
                .isInstanceOf(DocumentNotAvailableException.class)
                .hasMessageContaining("ya fue confirmado");
    }

    private DocumentModel document(DocumentStatusEnum status, DisbursementPolicyCat policy, String amount,
            UserModel uploader, int uploadOrder) {
        MasterAgreementModel agreement = MasterAgreementModel.builder()
                .id(UUID.randomUUID()).payer(payer).supplier(supplier).disbursementPolicy(policy).build();
        UploadBatchModel upload = UploadBatchModel.builder()
                .id(UUID.randomUUID())
                .uploadedAndApprovedBy(uploader)
                .createdAt(Instant.parse("2026-09-01T00:00:00Z").plusSeconds(uploadOrder))
                .build();
        return DocumentModel.builder()
                .id(UUID.randomUUID())
                .controlNumber("DTE-03-S001P002-00000000000" + uploadOrder)
                .dueDate(DUE_DATE)
                .nominalAmount(new BigDecimal(amount))
                .status(status)
                .masterAgreement(agreement)
                .uploadBatch(upload)
                .build();
    }

    private DispersionBatchModel batch(DispersionBatchStatusEnum status) {
        return DispersionBatchModel.builder()
                .id(UUID.randomUUID())
                .batchNumber("DSP-20261002-ABC123")
                .payer(payer)
                .payerAccountNumber("1000200030")
                .dueDate(DUE_DATE)
                .dispersionDate(DUE_DATE)
                .documentCount(2)
                .totalAmount(new BigDecimal("300.00"))
                .status(status)
                .createdBy(admin)
                .build();
    }

    private static BankAccountModel account(EntityModel owner, String number) {
        return BankAccountModel.builder().id(UUID.randomUUID()).entityModel(owner).accountNumber(number).isMain(true)
                .build();
    }
}
