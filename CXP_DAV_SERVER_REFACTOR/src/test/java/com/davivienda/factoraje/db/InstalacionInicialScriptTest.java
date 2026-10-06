package com.davivienda.factoraje.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Producción no usa Flyway: la base se crea con db/prod/instalacion_inicial.sql. Esta prueba
 * ejecuta el script con psql en una base y las migraciones con Flyway en otra, y exige que el
 * esquema y los catálogos coincidan.
 */
@Testcontainers(disabledWithoutDocker = true)
class InstalacionInicialScriptTest {

    private static final Path SCRIPT = Path.of("db/prod/instalacion_inicial.sql");
    /** Scripts de db/prod/actualizaciones posteriores a V8, en orden de aplicación. */
    private static final List<Path> UPDATES_AFTER_V8 = List.of(
            Path.of("db/prod/actualizaciones/V9__recursos_de_carga.sql"),
            Path.of("db/prod/actualizaciones/V10__manual_de_carga_pdf.sql"),
            Path.of("db/prod/actualizaciones/V11__bitacora_de_documentos_operador.sql"),
            Path.of("db/prod/actualizaciones/V12__parametros_de_mailjet.sql"),
            Path.of("db/prod/actualizaciones/V13__url_de_mailjet.sql"),
            Path.of("db/prod/actualizaciones/V14__secreto_jwt.sql"),
            Path.of("db/prod/actualizaciones/V15__valor_por_defecto_jwt_secret.sql"));

    private static final List<String> SCHEMA_QUERIES = List.of(
            """
            SELECT table_name, column_name, data_type, character_maximum_length,
                   numeric_precision, numeric_scale, is_nullable, column_default
            FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
            ORDER BY table_name, column_name
            """,
            """
            SELECT conrelid::regclass::text AS table_name, conname, pg_get_constraintdef(oid) AS definition
            FROM pg_constraint
            WHERE connamespace = 'public'::regnamespace AND conrelid::regclass::text <> 'flyway_schema_history'
            ORDER BY 1, 2
            """,
            """
            SELECT tablename, indexname, indexdef
            FROM pg_indexes
            WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
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
    private static final String SCRIPT_IN_CONTAINER = "/tmp/instalacion_inicial.sql";
    private static final Pattern PARAMETER = Pattern.compile("^\\\\set (\\w+)(\\s+)'COMPLETAR'$", Pattern.MULTILINE);

    private static final Map<String, String> PARAMETERS = Map.ofEntries(
            Map.entry("cors_origenes", "https://pay.davivienda.com.sv, https://pay2.davivienda.com.sv"),
            Map.entry("banco_nit", "0614-010190-101-1"),
            Map.entry("banco_nombre", "Banco Davivienda Salvadoreño"),
            Map.entry("banco_codigo", "davivienda"),
            Map.entry("operador_dui", "01234567-8"),
            Map.entry("operador_correo", "Operador@Davivienda.com.sv"),
            Map.entry("operador_nombres", "Ana"),
            Map.entry("operador_apellidos", "Operadora"),
            Map.entry("sysadmin_dui", "87654321-0"),
            Map.entry("sysadmin_correo", "sistemas@davivienda.com.sv"),
            Map.entry("sysadmin_nombres", "Luis"),
            Map.entry("sysadmin_apellidos", "Sistemas"));

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.9");

    private static JdbcTemplate flyway;
    private static JdbcTemplate script;

    @BeforeAll
    static void createBothDatabases() throws Exception {
        JdbcTemplate admin = jdbc(POSTGRES.getDatabaseName());
        admin.execute("CREATE DATABASE flyway_ref");
        admin.execute("CREATE DATABASE instalacion");

        Flyway.configure()
                .dataSource(url("flyway_ref"), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        flyway = jdbc("flyway_ref");

        POSTGRES.copyFileToContainer(Transferable.of(scriptWithParameters()), SCRIPT_IN_CONTAINER);
        ExecResult result = psql("instalacion");
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        script = jdbc("instalacion");
    }

    @Test
    void schemaMatchesFlywayMigrations() {
        assertSameResults(script, SCHEMA_QUERIES);
    }

    @Test
    void catalogsMatchFlywayMigrations() {
        assertSameResults(script, CATALOG_QUERIES);
    }

    @Test
    void updateScriptsBringAnInstallationAtV8ToTheLatestMigration() throws Exception {
        jdbc(POSTGRES.getDatabaseName()).execute("CREATE DATABASE actualizacion");
        Flyway.configure()
                .dataSource(url("actualizacion"), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target("8")
                .load()
                .migrate();

        for (Path update : UPDATES_AFTER_V8) {
            ExecResult result = applyUpdate(update, "actualizacion");
            assertThat(result.getExitCode()).as(update + ": " + result.getStderr()).isZero();
        }

        JdbcTemplate updated = jdbc("actualizacion");
        assertSameResults(updated, SCHEMA_QUERIES);
        assertSameResults(updated, CATALOG_QUERIES);

        for (Path update : UPDATES_AFTER_V8) {
            ExecResult second = applyUpdate(update, "actualizacion");
            assertThat(second.getExitCode()).as(update.toString()).isNotZero();
            assertThat(second.getStderr()).as(update.toString()).contains("ya está aplicada");
        }
    }

    private static ExecResult applyUpdate(Path update, String database) throws Exception {
        String inContainer = "/tmp/" + update.getFileName();
        POSTGRES.copyFileToContainer(Transferable.of(Files.readString(update, StandardCharsets.UTF_8)), inContainer);
        return POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", database, "-f", inContainer);
    }

    private static void assertSameResults(JdbcTemplate actual, List<String> queries) {
        for (String query : queries) {
            assertThat(actual.queryForList(query)).as(query).isEqualTo(flyway.queryForList(query));
        }
    }

    @Test
    void holidaysAreTheOfficialOnesWithoutTestData() {
        String holidays = "SELECT id, description, holiday_date, status FROM bank_holidays_cat ORDER BY id";

        assertThat(script.queryForList(holidays))
                .isEqualTo(flyway.queryForList(holidays.replace("ORDER BY", "WHERE description <> 'Dia de bukele' ORDER BY")))
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
    void refusesToRunOnANonEmptyDatabase() throws Exception {
        ExecResult second = psql("instalacion");

        assertThat(second.getExitCode()).isNotZero();
        assertThat(second.getStderr()).contains("La base no está vacía");
        assertThat(script.queryForObject("SELECT count(*) FROM users", Integer.class)).isEqualTo(2);
    }

    @Test
    void refusesToRunWithMissingParameters() throws Exception {
        jdbc(POSTGRES.getDatabaseName()).execute("CREATE DATABASE sin_parametros");
        POSTGRES.copyFileToContainer(Transferable.of(Files.readString(SCRIPT, StandardCharsets.UTF_8)), "/tmp/sin_parametros.sql");

        ExecResult result = POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", "sin_parametros",
                "-f", "/tmp/sin_parametros.sql");

        assertThat(result.getExitCode()).isNotZero();
        assertThat(result.getStderr()).contains("Faltan parámetros por completar");
        assertThat(jdbc("sin_parametros").queryForObject(
                "SELECT count(*) FROM pg_tables WHERE schemaname = 'public'", Integer.class)).isZero();
    }

    private static String scriptWithParameters() throws IOException {
        String content = Files.readString(SCRIPT, StandardCharsets.UTF_8);
        Matcher matcher = PARAMETER.matcher(content);
        StringBuilder filled = new StringBuilder();
        while (matcher.find()) {
            String value = PARAMETERS.get(matcher.group(1));
            assertThat(value).as("valor de prueba para " + matcher.group(1)).isNotNull();
            matcher.appendReplacement(filled, Matcher.quoteReplacement(
                    "\\set " + matcher.group(1) + matcher.group(2) + "'" + value + "'"));
        }
        matcher.appendTail(filled);
        assertThat(filled.toString()).doesNotContain("'COMPLETAR'\n");
        return filled.toString();
    }

    private static ExecResult psql(String database) throws Exception {
        return POSTGRES.execInContainer("psql", "-U", POSTGRES.getUsername(), "-d", database, "-f", SCRIPT_IN_CONTAINER);
    }

    private static String url(String database) {
        return "jdbc:postgresql://" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                + "/" + database;
    }

    private static JdbcTemplate jdbc(String database) {
        return new JdbcTemplate(new DriverManagerDataSource(url(database), POSTGRES.getUsername(), POSTGRES.getPassword()));
    }
}
