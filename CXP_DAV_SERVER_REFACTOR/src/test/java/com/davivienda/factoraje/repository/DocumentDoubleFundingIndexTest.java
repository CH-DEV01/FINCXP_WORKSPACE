package com.davivienda.factoraje.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.davivienda.factoraje.db.DeliverableScripts;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.InvoiceTypeEnum;
import com.davivienda.factoraje.domain.enums.IssuanceMethodEnum;

import jakarta.persistence.EntityManager;

/**
 * El esquema lo crea el script de instalación inicial que se entrega al DBA, así que la
 * prueba también cubre sus índices.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class DocumentDoubleFundingIndexTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.9");

    @BeforeAll
    static void installSchema() throws Exception {
        assumeTrue(DeliverableScripts.available(), "No existe la carpeta Entregables con los scripts de base de datos.");
        ExecResult installation = DeliverableScripts.run(POSTGRES, POSTGRES.getDatabaseName(), "instalacion_inicial.sql",
                DeliverableScripts.installationWithTestParameters());
        assertThat(installation.getExitCode()).as(installation.getStderr()).isZero();
    }

    @Autowired
    private DocumentRepository documentRepository;
    @Autowired
    private DisbursementBatchRepository disbursementBatchRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final UUID masterAgreementId = UUID.randomUUID();
    private final UUID uploadBatchId = UUID.randomUUID();

    @BeforeEach
    void disableForeignKeys() {
        // Solo interesan los índices únicos: se omiten las llaves foráneas en esta transacción
        // para no tener que crear pagador, proveedor, convenio y carga.
        jdbcTemplate.execute("SET LOCAL session_replication_role = replica");
    }

    @Test
    void uploadTemplateNoLongerUsesSupplierNiuNorOperatorAndOnlyPayersMayHaveNiu() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT logical_dto_field FROM excel_template_columns WHERE is_active", String.class))
                .contains("supplierNit", "supplierName", "supplierAccountNumber")
                .doesNotContain("supplierNiu", "operatorDui", "operatorEmail", "operatorFirstName",
                        "operatorMiddleName", "operatorFirstLastName", "operatorSecondLastName");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns WHERE table_name = 'entities' AND column_name = 'niu'",
                String.class))
                .isEqualTo("YES");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.table_constraints"
                        + " WHERE table_name = 'entities' AND constraint_name = 'uk_entities_niu' AND constraint_type = 'UNIQUE'",
                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void operatorRoleNoLongerExists() {
        assertThat(jdbcTemplate.queryForList("SELECT name FROM roles_cat", String.class))
                .contains("ADMIN", "SYSTEM_ADMIN", "PAYER", "SUPPLIER")
                .doesNotContain("OPERATOR");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT description FROM roles_cat WHERE name = 'ADMIN'", String.class))
                .isEqualTo("Operador bancario");
    }

    @Test
    void generationCodeIsUniqueIgnoringCase() {
        documentRepository.saveAndFlush(digital("ab12cd34-0000-4000-8000-000000000001", "DTE-03-ABCD1234-000000000000001",
                "A".repeat(40)));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(digital("AB12CD34-0000-4000-8000-000000000001",
                "DTE-03-ABCD1234-000000000000002", "B".repeat(40))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_documents_generation_code");
    }

    @Test
    void controlNumberIsUniqueIgnoringCase() {
        documentRepository.saveAndFlush(digital("AB12CD34-0000-4000-8000-000000000001", "DTE-03-abcd1234-000000000000001",
                "A".repeat(40)));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(digital("AB12CD34-0000-4000-8000-000000000002",
                "DTE-03-ABCD1234-000000000000001", "B".repeat(40))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_documents_control_number");
    }

    @Test
    void receivedStampIsUniqueIgnoringCase() {
        documentRepository.saveAndFlush(digital("AB12CD34-0000-4000-8000-000000000001", "DTE-03-ABCD1234-000000000000001",
                "a".repeat(40)));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(digital("AB12CD34-0000-4000-8000-000000000002",
                "DTE-03-ABCD1234-000000000000002", "A".repeat(40))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_documents_received_stamp");
    }

    @Test
    void paperDocumentNumberIsUniquePerAgreementAndYear() {
        documentRepository.saveAndFlush(paper("F-100", LocalDate.of(2025, 3, 1)));
        documentRepository.saveAndFlush(paper("F-100", LocalDate.of(2026, 3, 1)));

        assertThatThrownBy(() -> documentRepository.saveAndFlush(paper("F-100", LocalDate.of(2026, 7, 15))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_documents_paper_number_year");
    }

    @Test
    void aDocumentInactivatedByThePayerCanBeUploadedAgain() {
        DocumentModel inactivated = digital("AB12CD34-0000-4000-8000-000000000001", "DTE-03-ABCD1234-000000000000001",
                "A".repeat(40));
        inactivated.setStatus(DocumentStatusEnum.INACTIVATED_BY_PAYER);
        documentRepository.saveAndFlush(inactivated);
        DocumentModel inactivatedPaper = paper("F-200", LocalDate.of(2026, 3, 1));
        inactivatedPaper.setStatus(DocumentStatusEnum.INACTIVATED_BY_PAYER);
        documentRepository.saveAndFlush(inactivatedPaper);

        assertThat(documentRepository.findExistingGenerationCodes(List.of("AB12CD34-0000-4000-8000-000000000001"))).isEmpty();
        assertThat(documentRepository.findExistingControlNumbers(List.of("DTE-03-ABCD1234-000000000000001"))).isEmpty();
        assertThat(documentRepository.findExistingReceivedStamps(List.of("A".repeat(40)))).isEmpty();

        documentRepository.saveAndFlush(digital("AB12CD34-0000-4000-8000-000000000001", "DTE-03-ABCD1234-000000000000001",
                "A".repeat(40)));
        documentRepository.saveAndFlush(paper("F-200", LocalDate.of(2026, 3, 1)));

        assertThat(documentRepository.findExistingGenerationCodes(List.of("AB12CD34-0000-4000-8000-000000000001")))
                .containsExactly("AB12CD34-0000-4000-8000-000000000001");
    }

    @Test
    void aQuarantinedDocumentStillBlocksTheUpload() {
        DocumentModel quarantined = digital("AB12CD34-0000-4000-8000-000000000001", "DTE-03-ABCD1234-000000000000001",
                "A".repeat(40));
        quarantined.setStatus(DocumentStatusEnum.IN_QUARANTINE);
        documentRepository.saveAndFlush(quarantined);

        assertThat(documentRepository.findExistingControlNumbers(List.of("DTE-03-ABCD1234-000000000000001"))).hasSize(1);
        assertThatThrownBy(() -> documentRepository.saveAndFlush(digital("AB12CD34-0000-4000-8000-000000000001",
                "DTE-03-ABCD1234-000000000000001", "A".repeat(40))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void eachDisbursementBatchIsRepaidToTheCreditFacilityOnlyOnce() {
        assertThat(disbursementBatchRepository.findRestorableByPayer(UUID.randomUUID())).isEmpty();
        assertThat(disbursementBatchRepository.sumNominalAmount(UUID.randomUUID())).isEqualByComparingTo("0");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'uk_credit_facility_histories_disbursement_batch'",
                String.class))
                .contains("UNIQUE")
                .contains("(disbursement_batch_id)")
                .contains("WHERE (disbursement_batch_id IS NOT NULL)");
    }

    private DocumentModel digital(String generationCode, String controlNumber, String receivedStamp) {
        DocumentModel document = base(LocalDate.of(2026, 9, 1));
        document.setIssuanceMethod(IssuanceMethodEnum.DIGITAL);
        document.setGenerationCode(generationCode);
        document.setControlNumber(controlNumber);
        document.setReceivedStamp(receivedStamp);
        return document;
    }

    private DocumentModel paper(String documentNumber, LocalDate issueDate) {
        DocumentModel document = base(issueDate);
        document.setIssuanceMethod(IssuanceMethodEnum.PAPER);
        document.setDocumentNumber(documentNumber);
        return document;
    }

    private DocumentModel base(LocalDate issueDate) {
        return DocumentModel.builder()
                .issueDate(issueDate)
                .dueDate(issueDate.plusDays(90))
                .nominalAmount(new BigDecimal("100.00"))
                .invoiceType(InvoiceTypeEnum.CCF)
                .status(DocumentStatusEnum.APPROVED)
                .masterAgreement(entityManager.getReference(MasterAgreementModel.class, masterAgreementId))
                .uploadBatch(entityManager.getReference(UploadBatchModel.class, uploadBatchId))
                .build();
    }
}
