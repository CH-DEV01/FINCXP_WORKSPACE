package com.davivienda.factoraje.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;

/**
 * Scripts de base de datos que se entregan al DBA, en la carpeta Entregables de la raíz
 * del repositorio. La carpeta no se versiona: sin ella, las pruebas que la usan se omiten.
 */
public final class DeliverableScripts {

    private static final Path ROOT = Path.of("../Entregables");
    public static final Path INSTALLATION_DIR = ROOT.resolve("01_Puesta_en_marcha/01_Script_inicial");
    public static final Path SCHEMA = INSTALLATION_DIR.resolve("01_esquema.sql");
    public static final Path DATA = INSTALLATION_DIR.resolve("02_datos_iniciales.sql");
    public static final Path UPDATES = ROOT.resolve("02_Actualizaciones");
    /** Base en la versión 8, la primera que se entregó a producción; las actualizaciones parten de aquí. */
    public static final Path BASE_V8 = Path.of("src/test/resources/db/base_v8.sql");
    public static final int FIRST_UPDATE = 9;

    private static final Pattern UPDATE_NAME = Pattern.compile("V(\\d+)__\\w+\\.sql");
    private static final String PARAMETERS_START = "INSERT INTO instalacion_parametros";
    private static final Pattern PARAMETERS_END = Pattern.compile("\\);[ \\t]*$", Pattern.MULTILINE);
    private static final Pattern PARAMETER = Pattern.compile("^(\\s*\\('(\\w+)',\\s*)'[^']*'", Pattern.MULTILINE);

    public static final Map<String, String> TEST_PARAMETERS = Map.ofEntries(
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

    private DeliverableScripts() {
    }

    public static boolean available() {
        return Files.isRegularFile(SCHEMA) && Files.isRegularFile(DATA) && Files.isDirectory(UPDATES);
    }

    /** Scripts V{n}__descripcion.sql de la carpeta de actualizaciones, en orden de aplicación. */
    public static List<Path> updates() throws IOException {
        try (Stream<Path> files = Files.list(UPDATES)) {
            return files.filter(file -> UPDATE_NAME.matcher(file.getFileName().toString()).matches())
                    .sorted(Comparator.comparingInt(DeliverableScripts::version))
                    .toList();
        }
    }

    public static int version(Path update) {
        Matcher matcher = UPDATE_NAME.matcher(update.getFileName().toString());
        assertThat(matcher.matches()).as(update.toString()).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    /** Script de datos iniciales con los valores de prueba en la sección PARÁMETROS. */
    public static String dataWithTestParameters() throws IOException {
        return replaceParameters(name -> {
            String value = TEST_PARAMETERS.get(name);
            assertThat(value).as("valor de prueba para " + name).isNotNull();
            return value;
        });
    }

    /** Script de datos iniciales con la sección PARÁMETROS sin completar. */
    public static String dataWithoutParameters() throws IOException {
        return replaceParameters(name -> "COMPLETAR");
    }

    private static String replaceParameters(Function<String, String> valueOf) throws IOException {
        String content = Files.readString(DATA, StandardCharsets.UTF_8);
        int start = content.indexOf(PARAMETERS_START);
        assertThat(start).as("sección PARÁMETROS de " + DATA).isNotNegative();
        // Los comentarios de la sección también pueden tener punto y coma.
        Matcher sectionEnd = PARAMETERS_END.matcher(content);
        assertThat(sectionEnd.find(start)).as("fin de la sección PARÁMETROS de " + DATA).isTrue();
        int end = sectionEnd.start();

        Matcher matcher = PARAMETER.matcher(content.substring(start, end));
        StringBuilder section = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(section, Matcher.quoteReplacement(
                    matcher.group(1) + "'" + valueOf.apply(matcher.group(2)) + "'"));
        }
        matcher.appendTail(section);
        return content.substring(0, start) + section + content.substring(end);
    }

    /** Ejecuta el SQL con psql dentro del contenedor y se detiene en el primer error. */
    public static ExecResult run(PostgreSQLContainer<?> postgres, String database, String name, String sql) throws Exception {
        String inContainer = "/tmp/" + name;
        postgres.copyFileToContainer(Transferable.of(sql), inContainer);
        return postgres.execInContainer("psql", "-U", postgres.getUsername(), "-d", database,
                "-v", "ON_ERROR_STOP=1", "-f", inContainer);
    }

    public static ExecResult run(PostgreSQLContainer<?> postgres, String database, Path script) throws Exception {
        return run(postgres, database, script.getFileName().toString(), Files.readString(script, StandardCharsets.UTF_8));
    }

    /** Esquema y luego datos iniciales, con los valores de prueba. */
    public static void install(PostgreSQLContainer<?> postgres, String database) throws Exception {
        ExecResult schema = run(postgres, database, SCHEMA);
        assertThat(schema.getExitCode()).as(schema.getStderr()).isZero();
        ExecResult data = run(postgres, database, "02_datos_iniciales.sql", dataWithTestParameters());
        assertThat(data.getExitCode()).as(data.getStderr()).isZero();
    }
}
