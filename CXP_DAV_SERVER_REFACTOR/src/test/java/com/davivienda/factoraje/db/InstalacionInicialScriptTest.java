package com.davivienda.factoraje.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * La base se crea con los scripts de instalación inicial (esquema y datos) y los cambios
 * posteriores se entregan como scripts de actualización. Esta prueba instala esos scripts
 * en una base y, en otra, aplica todas las actualizaciones en orden sobre una base en la
 * versión 8, y exige que el esquema y los catálogos coincidan.
 */
@Testcontainers(disabledWithoutDocker = true)
class InstalacionInicialScriptTest {

    private static final List<String> SCHEMA_QUERIES = List.of(
            """
            SELECT table_name, column_name, data_type, character_maximum_length,
                   numeric_precision, numeric_scale, is_nullable, column_default
            FROM information_schema.columns
            WHERE table_schema = 'public'
            ORDER BY table_name, column_name
            """,
            """
            SELECT conrelid::regclass::text AS table_name, conname, pg_get_constraintdef(oid) AS definition
            FROM pg_constraint
            WHERE connamespace = 'public'::regnamespace
            ORDER BY 1, 2
            """,
            """
            SELECT tablename, indexname, indexdef
            FROM pg_indexes
            WHERE schemaname = 'public'
            ORDER BY 1, 2
            """);

    private static final List<String> CATALOG_QUERIES = List.of(
            "SELECT id, code, name, status FROM entity_types_cat ORDER BY id",
            "SELECT id, name, description, status, default_route FROM roles_cat ORDER BY id",
            "SELECT id, code, days_count, description, status FROM payment_policies_cat ORDER BY id",
            "SELECT id, code, name, description, status, offset_days, type, weekdays FROM disbursement_policies_cat ORDER BY id",
            "SELECT id, excel_column_name, logical_dto_field, is_active, is_required FROM excel_template_columns ORDER BY id",
            "SELECT id, term_name, unique_code, status FROM term_types_cat ORDER BY id",
            """
            SELECT id, term_type_id, version_number, status, publication_date, title, content,
                   acceptance_text, content_hash, document_url, published_by_id
            FROM term_versions_cat ORDER BY id
            """,
            "SELECT id, component_name, description, path, status FROM routes_cat ORDER BY id",
            "SELECT id, role_id, route_id, is_index FROM role_routes ORDER BY id",
            "SELECT id, label, path, icon, description, status FROM menus_cat ORDER BY id",
            "SELECT id, role_id, menu_id, display_order FROM role_menus ORDER BY id",
            "SELECT id, resource_type, file_name, file_size FROM upload_resources ORDER BY id",
            "SELECT id, param_key, param_value FROM system_parameters WHERE param_key <> 'CORS_ALLOWED_ORIGINS' ORDER BY id");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.9");

    private static JdbcTemplate script;
    private static JdbcTemplate updated;

    @BeforeAll
    static void createBothDatabases() throws Exception {
        assumeTrue(DeliverableScripts.available(), "No existe la carpeta Entregables con los scripts de base de datos.");
        JdbcTemplate admin = jdbc(POSTGRES.getDatabaseName());
        admin.execute("CREATE DATABASE instalacion");
        admin.execute("CREATE DATABASE actualizacion");

        DeliverableScripts.install(POSTGRES, "instalacion");
        script = jdbc("instalacion");

        ExecResult base = DeliverableScripts.run(POSTGRES, "actualizacion", DeliverableScripts.BASE_V8);
        assertThat(base.getExitCode()).as(base.getStderr()).isZero();
        for (Path update : DeliverableScripts.updates()) {
            ExecResult result = DeliverableScripts.run(POSTGRES, "actualizacion", update);
            assertThat(result.getExitCode()).as(update + ": " + result.getStderr()).isZero();
        }
        updated = jdbc("actualizacion");
    }

    @Test
    void updatesAreNumberedConsecutively() throws Exception {
        List<Integer> versions = DeliverableScripts.updates().stream().map(DeliverableScripts::version).toList();

        assertThat(versions).isEqualTo(IntStream.rangeClosed(DeliverableScripts.FIRST_UPDATE,
                DeliverableScripts.FIRST_UPDATE + versions.size() - 1).boxed().toList());
    }

    @Test
    void schemaMatchesAnInstallationBroughtUpToDateWithTheUpdates() {
        assertSameResults(SCHEMA_QUERIES);
    }

    @Test
    void catalogsMatchAnInstallationBroughtUpToDateWithTheUpdates() {
        assertSameResults(CATALOG_QUERIES);
    }

    @Test
    void updatesRefuseToRunTwice() throws Exception {
        for (Path update : DeliverableScripts.updates()) {
            ExecResult second = DeliverableScripts.run(POSTGRES, "actualizacion", update);
            assertThat(second.getExitCode()).as(update.toString()).isNotZero();
            assertThat(second.getStderr()).as(update.toString()).contains("ya está aplicada");
        }
    }

    private static void assertSameResults(List<String> queries) {
        for (String query : queries) {
            assertThat(script.queryForList(query)).as(query).isEqualTo(updated.queryForList(query));
        }
    }

    @Test
    void holidaysAreTheOfficialOnesWithoutTestData() {
        String holidays = "SELECT id, description, holiday_date, status FROM bank_holidays_cat ORDER BY id";

        assertThat(script.queryForList(holidays))
                .isEqualTo(updated.queryForList(holidays.replace("ORDER BY", "WHERE description <> 'Dia de bukele' ORDER BY")))
                .hasSize(6);
    }

    @Test
    void termHashesMatchTheirContent() {
        assertThat(script.queryForObject("""
                SELECT count(*) FROM term_versions_cat
                WHERE content_hash <> encode(sha256(convert_to(title || E'\\n\\n' || content || E'\\n\\n' || acceptance_text, 'UTF8')), 'hex')
                """, Integer.class)).isZero();
    }

    @Test
    void createsTheBankAndTheMotherUsersWithNormalizedData() {
        assertThat(script.queryForMap("SELECT e.code, e.nit, e.name, e.status, t.code AS type FROM entities e JOIN entity_types_cat t ON t.id = e.entity_type_id"))
                .containsEntry("code", "DAVIVIENDA")
                .containsEntry("nit", "06140101901011")
                .containsEntry("name", "Banco Davivienda Salvadoreño")
                .containsEntry("status", "ACTIVE")
                .containsEntry("type", "COD_003");

        assertThat(script.queryForList("""
                SELECT r.name AS role, u.dui, u.email, u.status
                FROM users u JOIN roles_cat r ON r.id = u.role_id ORDER BY r.name
                """)).containsExactly(
                Map.of("role", "ADMIN", "dui", "012345678", "email", "operador@davivienda.com.sv", "status", "ACTIVE"),
                Map.of("role", "SYSTEM_ADMIN", "dui", "876543210", "email", "sistemas@davivienda.com.sv", "status", "ACTIVE"));

        assertThat(script.queryForObject("SELECT param_value FROM system_parameters WHERE param_key = 'CORS_ALLOWED_ORIGINS'", String.class))
                .isEqualTo("https://pay.davivienda.com.sv,https://pay2.davivienda.com.sv");
    }

    @Test
    void refusesToCreateTheSchemaOnANonEmptyDatabase() throws Exception {
        ExecResult second = DeliverableScripts.run(POSTGRES, "instalacion", DeliverableScripts.SCHEMA);

        assertThat(second.getExitCode()).isNotZero();
        assertThat(second.getStderr()).contains("La base no está vacía");
        assertThat(script.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(2);
    }

    @Test
    void refusesToLoadDataTwice() throws Exception {
        ExecResult second = DeliverableScripts.run(POSTGRES, "instalacion", "02_datos_iniciales.sql",
                DeliverableScripts.dataWithTestParameters());

        assertThat(second.getExitCode()).isNotZero();
        assertThat(second.getStderr()).contains("ya están cargados");
        assertThat(script.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(2);
    }

    @Test
    void refusesToLoadDataWithMissingParameters() throws Exception {
        jdbc(POSTGRES.getDatabaseName()).execute("CREATE DATABASE sin_parametros");
        ExecResult schema = DeliverableScripts.run(POSTGRES, "sin_parametros", DeliverableScripts.SCHEMA);
        assertThat(schema.getExitCode()).as(schema.getStderr()).isZero();

        ExecResult result = DeliverableScripts.run(POSTGRES, "sin_parametros", "sin_parametros.sql",
                DeliverableScripts.dataWithoutParameters());

        assertThat(result.getExitCode()).isNotZero();
        assertThat(result.getStderr()).contains("Faltan parámetros por completar");
        assertThat(jdbc("sin_parametros").queryForObject("SELECT count(*) FROM entity_types_cat", Integer.class))
                .isZero();
    }

    @Test
    void refusesToLoadDataWithoutTheSchema() throws Exception {
        jdbc(POSTGRES.getDatabaseName()).execute("CREATE DATABASE sin_esquema");

        ExecResult result = DeliverableScripts.run(POSTGRES, "sin_esquema", "sin_esquema.sql",
                DeliverableScripts.dataWithTestParameters());

        assertThat(result.getExitCode()).isNotZero();
        assertThat(result.getStderr()).contains("Falta el esquema");
        assertThat(jdbc("sin_esquema").queryForObject(
                "SELECT count(*) FROM pg_tables WHERE schemaname = 'public'", Integer.class)).isZero();
    }

    private static String url(String database) {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                + "/" + database;
    }

    private static JdbcTemplate jdbc(String database) {
        return new JdbcTemplate(new DriverManagerDataSource(url(database), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }
}
