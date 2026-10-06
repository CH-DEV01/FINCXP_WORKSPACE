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
                .thenReturn(List.of(PaymentPolicyCat.builder().code("P90").build()));
        lenient().when(disbursementPolicyCatRepository.findAll())
                .thenReturn(List.of(DisbursementPolicyCat.builder().code("FRIDAY").build()));
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
        when(documentRepository.findExistingPhysicalDocuments(SUPPLIER_NIT, List.of("F-001"), TODAY.minusDays(5).getYear()))
                .thenReturn(Set.of("F-001"));

        List<BatchValidationError> errors = service.validateBatch(List.of(paperInvoice("F-001")), null);

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.columnName()).isEqualTo("Numero de documento");
            assertThat(error.errorMessage()).contains("Doble Fondeo").contains("NIT: " + SUPPLIER_NIT);
        });
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
    void duplicatedDteInsideTheFileIsRejected() {
        InvoiceRecordDTO first = invoice(new BigDecimal("10"), "P90", "FRIDAY");

        List<BatchValidationError> errors = service.validateDteDuplicatesInFile(List.of(first, first));

        assertThat(errors).extracting(BatchValidationError::columnName)
                .containsExactly("Codigo de generacion", "Numero de control", "Sello de recepcion");
    }

    private static InvoiceRecordDTO invoice(BigDecimal amount, String paymentPolicy, String disbursementDay) {
        return new InvoiceRecordDTO(
                2,
                TODAY.minusDays(5),
                amount,
                null,
                GENERATION_CODE,
                "A".repeat(40),
                "DTE-03-ABCD1234-000000000000001",
                "DIGITAL",
                "CCF",
                SUPPLIER_NIT,
                "Proveedor de prueba",
                "0012345678",
                paymentPolicy,
                disbursementDay);
    }

    private static InvoiceRecordDTO paperInvoice(String documentNumber) {
        return new InvoiceRecordDTO(
                3,
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
