package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.catalogs.EntityTypeCat;
import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;
import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.upload_batch.InvoiceRecordDTO;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.file_parser.excel.BatchValidationError;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.repository.DocumentRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.repository.PaymentPolicyCatRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;

@ExtendWith(MockitoExtension.class)
class DocumentValidationServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 2);
    private static final String GENERATION_CODE = "A1B2C3D4-0000-4000-8000-000000000001";
    private static final String SUPPLIER_NIT = "06140101901011";

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private MasterAgreementRepository masterAgreementRepository;
    @Mock
    private EntityRepository entityRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private PaymentPolicyCatRepository paymentPolicyCatRepository;
    @Mock
    private DisbursementPolicyCatRepository disbursementPolicyCatRepository;
    @Mock
    private SystemParameters systemParameters;
    @Mock
    private DisbursementPolicyService disbursementPolicyService;

    @InjectMocks
    private DocumentValidationServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(disbursementPolicyService.businessToday()).thenReturn(TODAY);
        lenient().when(systemParameters.getInt(SystemParameterKey.MAX_INVOICE_AGE_DAYS)).thenReturn(365);
        lenient().when(paymentPolicyCatRepository.findAll())
                .thenReturn(List.of(
                        PaymentPolicyCat.builder().code("P90").daysCount(90).build(),
                        PaymentPolicyCat.builder().code("P30").daysCount(30).build()));
        lenient().when(disbursementPolicyCatRepository.findAll())
                .thenReturn(List.of(DisbursementPolicyCat.builder().code("FRIDAY").build()));
    }

    @Test
    void alreadyDueInvoiceIsRejectedOnUpload() {
        // Emisión hace 40 días + política de 30 → vencimiento hace 10 días.
        InvoiceRecordDTO pastDue = new InvoiceRecordDTO(
                2,
                TODAY.minusDays(40),
                new BigDecimal("1000.00"),
                null,
                GENERATION_CODE,
                "A".repeat(40),
                "DTE-03-ABCD1234-000000000000001",
                "DIGITAL",
                "CCF",
                SUPPLIER_NIT,
                "Proveedor de prueba",
                "0012345678",
                "P30",
                "FRIDAY");

        List<BatchValidationError> errors = service.validateBatch(List.of(pastDue), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.columnName()).isEqualTo("Fecha de emision");
            assertThat(error.errorMessage()).contains("ya está vencido").contains("30 días");
        });
    }

    @Test
    void invoiceDueTodayIsStillAccepted() {
        // Emisión hace 30 días + política de 30 → vence hoy; aún no está vencido.
        InvoiceRecordDTO dueToday = new InvoiceRecordDTO(
                2,
                TODAY.minusDays(30),
                new BigDecimal("1000.00"),
                null,
                GENERATION_CODE,
                "A".repeat(40),
                "DTE-03-ABCD1234-000000000000001",
                "DIGITAL",
                "CCF",
                SUPPLIER_NIT,
                "Proveedor de prueba",
                "0012345678",
                "P30",
                "FRIDAY");

        assertThat(service.validateBatch(List.of(dueToday), null)).isEmpty();
    }

    @Test
    void validDigitalInvoiceHasNoErrors() {
        assertThat(service.validateBatch(List.of(invoice(new BigDecimal("1500.25"), "P90", "FRIDAY")), null))
                .isEmpty();
    }

    @Test
    void amountWithMoreThanTwoDecimalsIsRejected() {
        List<BatchValidationError> errors = service.validateBatch(
                List.of(invoice(new BigDecimal("1500.255"), "P90", "FRIDAY")), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.rowIndex()).isEqualTo(2);
            assertThat(error.columnName()).isEqualTo("Monto");
            assertThat(error.errorMessage()).contains("máximo 2 decimales").contains("1500.255");
        });
    }

    @Test
    void trailingZerosDoNotCountAsExtraDecimals() {
        assertThat(service.validateBatch(List.of(invoice(new BigDecimal("1500.2500"), "P90", "FRIDAY")), null))
                .isEmpty();
    }

    @Test
    void missingAmountIsRejected() {
        List<BatchValidationError> errors = service.validateBatch(List.of(invoice(null, "P90", "FRIDAY")), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.columnName()).isEqualTo("Monto");
            assertThat(error.errorMessage()).contains("obligatorio");
        });
    }

    @Test
    void nonPositiveAmountIsRejected() {
        List<BatchValidationError> errors = service.validateBatch(
                List.of(invoice(BigDecimal.ZERO, "P90", "FRIDAY")), null);

        assertThat(errors).extracting(BatchValidationError::errorMessage)
                .containsExactly("El monto nominal de la factura debe ser mayor a cero.");
    }

    @Test
    void unknownPolicyCodesAreRejected() {
        List<BatchValidationError> errors = service.validateBatch(List.of(invoice(new BigDecimal("10"), "P15", "MONDAY")), null);

        assertThat(errors).extracting(BatchValidationError::columnName)
                .containsExactlyInAnyOrder("Politica de pago", "Dia de desembolso");
        assertThat(errors).extracting(BatchValidationError::errorMessage)
                .anySatisfy(message -> assertThat(message).contains("política de pago no existe").endsWith("P15"))
                .anySatisfy(message -> assertThat(message).contains("día de desembolso no existe").endsWith("MONDAY"));
    }

    @Test
    void missingPolicyCodesAreRequired() {
        List<BatchValidationError> errors = service.validateBatch(List.of(invoice(new BigDecimal("10"), " ", null)), null);

        assertThat(errors).extracting(BatchValidationError::errorMessage)
                .containsOnly("El campo es obligatorio y no puede estar vacío.");
        assertThat(errors).hasSize(2);
    }

    @Test
    void generationCodeAlreadyRegisteredIsDoubleFunding() {
        when(documentRepository.findExistingGenerationCodes(anyList())).thenReturn(Set.of(GENERATION_CODE));

        List<BatchValidationError> errors = service.validateBatch(
                List.of(invoice(new BigDecimal("10"), "P90", "FRIDAY")), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.columnName()).isEqualTo("Codigo de generacion");
            assertThat(error.errorMessage()).contains("Doble Fondeo");
        });
    }

    @Test
    void paperDoubleFundingIsLookedUpBySupplierNit() {
        // "f-001" se normaliza a F001 antes de buscar en la base (DUP-01).
        when(documentRepository.findExistingPhysicalDocuments(SUPPLIER_NIT, List.of("F001"), TODAY.minusDays(5).getYear()))
                .thenReturn(Set.of("F001"));

        List<BatchValidationError> errors = service.validateBatch(List.of(paperInvoice("f-001")), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.columnName()).isEqualTo("Numero de documento");
            assertThat(error.errorMessage()).contains("Doble Fondeo").contains("NIT: " + SUPPLIER_NIT).contains("F001");
        });
    }

    @Test
    void paperDocumentVariantsInTheSameFileAreTreatedAsDuplicates() {
        List<BatchValidationError> errors = service.validateBatch(
                List.of(paperInvoice("F-001"), paperInvoiceRow(4, "F 001")), null);

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.columnName()).isEqualTo("Numero de documento");
            assertThat(error.errorMessage()).contains("duplicada");
        });
    }

    @Test
    void paperDocumentSameNumberInDifferentYearsIsAllowedInTheSameFile() {
        // Política larga para que una emisión del año anterior aún no esté vencida (FEC-03).
        when(paymentPolicyCatRepository.findAll())
                .thenReturn(List.of(PaymentPolicyCat.builder().code("P90").daysCount(400).build()));

        InvoiceRecordDTO priorYear = new InvoiceRecordDTO(
                4,
                TODAY.minusYears(1).plusMonths(1),
                new BigDecimal("10"),
                "F-001",
                null,
                null,
                null,
                "PAPER",
                "CCF",
                SUPPLIER_NIT,
                "Proveedor de prueba",
                "0012345678",
                "P90",
                "FRIDAY");

        assertThat(service.validateBatch(List.of(paperInvoice("F-001"), priorYear), null)).isEmpty();
    }

    @Test
    void nitRegisteredForANonSupplierEntityIsRejected() {
        EntityModel payer = EntityModel.builder()
                .nit(SUPPLIER_NIT)
                .name("Pagador S.A.")
                .entityType(EntityTypeCat.builder().code(EntityTypeCode.PAYER).build())
                .build();
        when(entityRepository.findByNit(SUPPLIER_NIT)).thenReturn(Optional.of(payer));

        List<BatchValidationError> errors = service.validateBatch(
                List.of(invoice(new BigDecimal("10"), "P90", "FRIDAY")), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.columnName()).isEqualTo("NIT del proveedor");
            assertThat(error.errorMessage()).contains("Pagador S.A.").contains("no es un proveedor");
        });
    }

    @Test
    void newSupplierNameAlreadyRegisteredIsRejected() {
        when(entityRepository.existsByName("Proveedor de prueba")).thenReturn(true);

        List<BatchValidationError> errors = service.validateBatch(
                List.of(invoice(new BigDecimal("10"), "P90", "FRIDAY")), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.rowIndex()).isEqualTo(2);
            assertThat(error.columnName()).isEqualTo("Nombre del proveedor");
            assertThat(error.errorMessage()).contains("Ya existe una entidad registrada")
                    .contains("Proveedor de prueba");
        });
    }

    @Test
    void existingSupplierDoesNotRevalidateNameUniqueness() {
        EntityModel supplier = EntityModel.builder()
                .id(UUID.randomUUID())
                .nit(SUPPLIER_NIT)
                .name("Proveedor de prueba")
                .entityType(EntityTypeCat.builder().code(EntityTypeCode.SUPPLIER).build())
                .build();
        when(entityRepository.findByNit(SUPPLIER_NIT)).thenReturn(Optional.of(supplier));

        assertThat(service.validateBatch(List.of(invoice(new BigDecimal("10"), "P90", "FRIDAY")), null))
                .isEmpty();
    }

    @Test
    void supplierRowsWithDifferentNamesAreRejected() {
        InvoiceRecordDTO first = invoice(new BigDecimal("10"), "P90", "FRIDAY");
        InvoiceRecordDTO second = invoiceRow(4, "Otro nombre", "P90", "FRIDAY");

        List<BatchValidationError> errors = service.validateBatch(List.of(first, second), null);

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.rowIndex()).isEqualTo(4);
            assertThat(error.columnName()).isEqualTo("Nombre del proveedor");
            assertThat(error.errorMessage()).contains("mismo nombre").contains("Otro nombre");
        });
    }

    @Test
    void supplierRowsWithDifferentPaymentPoliciesAreRejected() {
        InvoiceRecordDTO first = invoice(new BigDecimal("10"), "P90", "FRIDAY");
        InvoiceRecordDTO second = invoiceRow(4, "Proveedor de prueba", "P30", "FRIDAY");

        List<BatchValidationError> errors = service.validateBatch(List.of(first, second), null);

        assertThat(errors).anySatisfy(error -> {
            assertThat(error.rowIndex()).isEqualTo(4);
            assertThat(error.columnName()).isEqualTo("Politica de pago");
            assertThat(error.errorMessage()).contains("misma política de pago").contains("P30");
        });
    }

    @Test
    void existingAgreementPoliciesMustMatchTheFile() {
        UUID payerId = UUID.randomUUID();
        UUID supplierId = UUID.randomUUID();
        EntityModel supplier = EntityModel.builder()
                .id(supplierId)
                .nit(SUPPLIER_NIT)
                .name("Proveedor de prueba")
                .entityType(EntityTypeCat.builder().code(EntityTypeCode.SUPPLIER).build())
                .build();
        when(entityRepository.findByNit(SUPPLIER_NIT)).thenReturn(Optional.of(supplier));
        when(masterAgreementRepository.findByPayerIdAndSupplierId(payerId, supplierId))
                .thenReturn(Optional.of(MasterAgreementModel.builder()
                        .paymentPolicy(PaymentPolicyCat.builder().code("P30").daysCount(30).build())
                        .disbursementPolicy(DisbursementPolicyCat.builder().code("FRIDAY").build())
                        .build()));

        List<BatchValidationError> errors = service.validateBatch(
                List.of(invoice(new BigDecimal("10"), "P90", "FRIDAY")), payerId);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.columnName()).isEqualTo("Politica de pago");
            assertThat(error.errorMessage()).contains("ya tiene la política de pago P30").contains("P90");
        });
    }

    @Test
    void twoNewNitsWithTheSameNameInTheFileAreRejected() {
        String otherNit = "06140101901022";
        InvoiceRecordDTO first = invoice(new BigDecimal("10"), "P90", "FRIDAY");
        InvoiceRecordDTO second = new InvoiceRecordDTO(
                4,
                TODAY.minusDays(5),
                new BigDecimal("20"),
                null,
                "B1B2C3D4-0000-4000-8000-000000000002",
                "B".repeat(40),
                "DTE-03-ABCD1234-000000000000002",
                "DIGITAL",
                "CCF",
                otherNit,
                "Proveedor de prueba",
                "0012345679",
                "P90",
                "FRIDAY");

        List<BatchValidationError> errors = service.validateNewSupplierNamesInFile(List.of(first, second));

        assertThat(errors).hasSize(2);
        assertThat(errors).allSatisfy(error -> {
            assertThat(error.columnName()).isEqualTo("Nombre del proveedor");
            assertThat(error.errorMessage()).contains("varios NIT nuevos").contains("Proveedor de prueba");
        });
        assertThat(errors).extracting(BatchValidationError::rowIndex).containsExactlyInAnyOrder(2, 4);
    }

    @Test
    void duplicatedDteInsideTheFileIsRejected() {
        InvoiceRecordDTO first = invoice(new BigDecimal("10"), "P90", "FRIDAY");

        List<BatchValidationError> errors = service.validateDteDuplicatesInFile(List.of(first, first));

        assertThat(errors).extracting(BatchValidationError::columnName)
                .containsExactly("Codigo de generacion", "Numero de control", "Sello de recepcion");
    }

    private static InvoiceRecordDTO invoice(BigDecimal amount, String paymentPolicy, String disbursementDay) {
        return invoiceRow(2, "Proveedor de prueba", paymentPolicy, disbursementDay, amount);
    }

    private static InvoiceRecordDTO invoiceRow(int rowIndex, String supplierName, String paymentPolicy,
            String disbursementDay) {
        return invoiceRow(rowIndex, supplierName, paymentPolicy, disbursementDay, new BigDecimal("10"));
    }

    private static InvoiceRecordDTO invoiceRow(int rowIndex, String supplierName, String paymentPolicy,
            String disbursementDay, BigDecimal amount) {
        return new InvoiceRecordDTO(
                rowIndex,
                TODAY.minusDays(5),
                amount,
                null,
                GENERATION_CODE,
                "A".repeat(40),
                "DTE-03-ABCD1234-000000000000001",
                "DIGITAL",
                "CCF",
                SUPPLIER_NIT,
                supplierName,
                "0012345678",
                paymentPolicy,
                disbursementDay);
    }

    private static InvoiceRecordDTO paperInvoice(String documentNumber) {
        return paperInvoiceRow(3, documentNumber);
    }

    private static InvoiceRecordDTO paperInvoiceRow(int rowIndex, String documentNumber) {
        return new InvoiceRecordDTO(
                rowIndex,
                TODAY.minusDays(5),
                new BigDecimal("10"),
                documentNumber,
                null,
                null,
                null,
                "PAPER",
                "CCF",
                SUPPLIER_NIT,
                "Proveedor de prueba",
                "0012345678",
                "P90",
                "FRIDAY");
    }
}
