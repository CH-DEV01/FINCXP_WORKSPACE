package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.dto.disbursement_batch.GenerateBatchRequestDTO;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.repository.DisbursementBatchRepository;
import com.davivienda.factoraje.repository.DocumentLogRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.FinancingTransactionRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.DisbursementBatchService.Letter;
import com.davivienda.factoraje.service.DisbursementLetterPdfService;
import com.davivienda.factoraje.service.DisbursementPolicyService;
import com.davivienda.factoraje.service.impl.DisbursementLetterAssembler.ReportContext;

class DisbursementBatchServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final LocalDate DUE_DATE = TODAY.plusDays(3);
    private static final LocalDate REQUEST_DATE = TODAY.minusDays(1);
    private static final LocalDate DISBURSEMENT_DATE = TODAY.plusDays(1);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final EntityRepository entityRepository = mock(EntityRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentLogRepository documentLogRepository = mock(DocumentLogRepository.class);
    private final FinancingTransactionRepository financingTransactionRepository =
            mock(FinancingTransactionRepository.class);
    private final DisbursementBatchRepository disbursementBatchRepository = mock(DisbursementBatchRepository.class);
    private final DisbursementPolicyService disbursementPolicyService = mock(DisbursementPolicyService.class);
    private final DisbursementLetterAssembler letterAssembler = mock(DisbursementLetterAssembler.class);
    private final DisbursementLetterPdfService disbursementLetterPdfService = mock(DisbursementLetterPdfService.class);
    private final MailNoticePublisher mailNotices = mock(MailNoticePublisher.class);

    private final UserModel operator = UserModel.builder().id(UUID.randomUUID()).firstName("Operador").build();
    private final EntityModel payer = EntityModel.builder().id(UUID.randomUUID()).name("Pagador Demo").build();

    private DisbursementBatchServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisbursementBatchServiceImpl(userRepository, entityRepository, documentRepository,
                documentLogRepository, financingTransactionRepository, disbursementBatchRepository,
                disbursementPolicyService, letterAssembler, disbursementLetterPdfService, mailNotices);

        lenient().when(disbursementPolicyService.businessToday()).thenReturn(TODAY);
        lenient().when(userRepository.findById(operator.getId())).thenReturn(Optional.of(operator));
        lenient().when(entityRepository.findByIdForUpdate(payer.getId())).thenReturn(Optional.of(payer));
        lenient().when(letterAssembler.loadReportContext(payer)).thenReturn(new ReportContext("Pagador Demo", "CF-1"));
        lenient().when(letterAssembler.reportFileName(any(DisbursementBatchModel.class)))
                .thenAnswer(invocation -> "Reporte_Desembolso_"
                        + invocation.getArgument(0, DisbursementBatchModel.class).getBatchNumber() + ".pdf");
        lenient().when(letterAssembler.assemble(any(), any(), any())).thenReturn(null);
        lenient().when(disbursementLetterPdfService.generate(any())).thenReturn(new byte[] {1});
        lenient().when(disbursementBatchRepository.existsByBatchNumber(any())).thenReturn(false);
        lenient().when(disbursementBatchRepository.save(any(DisbursementBatchModel.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void aSingleGroupReturnsThePdfLetter() {
        GenerateBatchRequestDTO.Group group = new GenerateBatchRequestDTO.Group(
                DUE_DATE, REQUEST_DATE, DISBURSEMENT_DATE);
        when(financingTransactionRepository.findPendingInGroup(payer.getId(),
                DocumentStatusEnum.REQUESTED_FOR_FINANCING, DUE_DATE, REQUEST_DATE, DISBURSEMENT_DATE))
                .thenReturn(List.of(transaction("100.00")));

        Letter letter = service.generateDisbursementBatches(operator.getId(), "origen.xlsx", payer.getId(),
                List.of(group));

        ArgumentCaptor<DisbursementBatchModel> batchCaptor = ArgumentCaptor.forClass(DisbursementBatchModel.class);
        verify(disbursementBatchRepository).save(batchCaptor.capture());
        assertThat(letter.fileName()).isEqualTo("Reporte_Desembolso_" + batchCaptor.getValue().getBatchNumber() + ".pdf");
        assertThat(letter.content()).isEqualTo(new byte[] {1});
    }

    @Test
    void severalGroupsReturnAZipWithOnePdfPerBatch() throws IOException {
        LocalDate laterDue = DUE_DATE.plusDays(1);
        GenerateBatchRequestDTO.Group first = new GenerateBatchRequestDTO.Group(
                DUE_DATE, REQUEST_DATE, DISBURSEMENT_DATE);
        GenerateBatchRequestDTO.Group second = new GenerateBatchRequestDTO.Group(
                laterDue, REQUEST_DATE, DISBURSEMENT_DATE);

        when(financingTransactionRepository.findPendingInGroup(eq(payer.getId()),
                eq(DocumentStatusEnum.REQUESTED_FOR_FINANCING), eq(DUE_DATE), eq(REQUEST_DATE), eq(DISBURSEMENT_DATE)))
                .thenReturn(List.of(transaction("100.00")));
        when(financingTransactionRepository.findPendingInGroup(eq(payer.getId()),
                eq(DocumentStatusEnum.REQUESTED_FOR_FINANCING), eq(laterDue), eq(REQUEST_DATE), eq(DISBURSEMENT_DATE)))
                .thenReturn(List.of(transaction("200.00")));
        when(disbursementLetterPdfService.generate(any()))
                .thenReturn(new byte[] {1}, new byte[] {2});

        Letter zip = service.generateDisbursementBatches(operator.getId(), "origen.xlsx", payer.getId(),
                List.of(second, first, first));

        ArgumentCaptor<DisbursementBatchModel> batchCaptor = ArgumentCaptor.forClass(DisbursementBatchModel.class);
        verify(disbursementBatchRepository, times(2)).save(batchCaptor.capture());
        List<DisbursementBatchModel> batches = batchCaptor.getAllValues();
        assertThat(batches).extracting(DisbursementBatchModel::getDueDate).containsExactly(laterDue, DUE_DATE);

        assertThat(zip.fileName()).isEqualTo("Reportes_Desembolso_20261002.zip");
        assertThat(zipEntries(zip.content())).containsExactly(
                "Reporte_Desembolso_" + batches.get(0).getBatchNumber() + ".pdf",
                "Reporte_Desembolso_" + batches.get(1).getBatchNumber() + ".pdf");
    }

    @Test
    void rejectsAnEmptySelection() {
        assertThatThrownBy(() -> service.generateDisbursementBatches(operator.getId(), "origen.xlsx", payer.getId(),
                List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static FinancingTransactionModel transaction(String amount) {
        DocumentModel document = DocumentModel.builder()
                .id(UUID.randomUUID())
                .status(DocumentStatusEnum.REQUESTED_FOR_FINANCING)
                .build();
        return FinancingTransactionModel.builder()
                .id(UUID.randomUUID())
                .interestAmount(new BigDecimal("1.00"))
                .commissionAmount(new BigDecimal("2.00"))
                .amountToBeDisbursed(new BigDecimal(amount))
                .document(document)
                .build();
    }

    private static List<String> zipEntries(byte[] content) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }
}
