package com.davivienda.factoraje.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.enums.DocumentStatusEnum;
import com.davivienda.factoraje.domain.enums.InvoiceTypeEnum;
import com.davivienda.factoraje.domain.enums.IssuanceMethodEnum;

import jakarta.persistence.EntityManager;

/**
 * El esquema lo crea Flyway (V1/V2) y Hibernate lo valida (ddl-auto=validate), así que
 * la prueba también cubre las migraciones.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class DocumentDoubleFundingIndexTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.9");

    @Autowired
    private DocumentRepository documentRepository;
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
    void flywayAppliedBothMigrations() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success AND version IN ('1', '2')", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void uploadTemplateNoLongerUsesSupplierNiuNorOperatorAndEntitiesHaveNoNiu() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT logical_dto_field FROM excel_template_columns WHERE is_active", String.class))
                .contains("supplierNit", "supplierName", "supplierAccountNumber")
                .doesNotContain("supplierNiu", "operatorDui", "operatorEmail", "operatorFirstName",
                        "operatorMiddleName", "operatorFirstLastName", "operatorSecondLastName");

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns WHERE table_name = 'entities' AND column_name = 'niu'",
                Integer.class))
                .isZero();
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
