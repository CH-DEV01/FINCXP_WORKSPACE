package com.davivienda.factoraje.domain.enums;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import com.davivienda.factoraje.infrastructure.util.IdentifierNormalizer;

/**
 * Parámetros de sistema conocidos por el backend. El valor por defecto se usa
 * cuando la fila no existe o su valor es inválido, para que un error de
 * configuración no detenga la operación.
 */
public enum SystemParameterKey {

    IVA_RATE(ValueType.DECIMAL, "0.13", "0", "1"),
    DEFAULT_CREDIT_THRESHOLD(ValueType.DECIMAL, "0.80", "0.01", "1"),
    DISBURSEMENT_CUTOFF_TIME(ValueType.TIME, "15:00", null, null),
    DUE_DATE_GRACE_DAYS(ValueType.INTEGER, "5", "0", "60"),
    MAX_INVOICE_AGE_DAYS(ValueType.INTEGER, "120", "1", "3650"),
    UPLOAD_ALLOWED_EXTENSIONS(ValueType.FILE_EXTENSIONS, ".xlsx,.xls", null, null),
    // El máximo debe coincidir con spring.servlet.multipart.max-file-size.
    UPLOAD_MAX_FILE_SIZE_MB(ValueType.INTEGER, "5", "1", "50"),
    UPLOAD_MAX_ROWS(ValueType.INTEGER, "5000", "1", "100000"),
    CORS_ALLOWED_ORIGINS(ValueType.ORIGINS, "http://localhost:5173,https://devpay.davivienda.com.sv", null, null),
    JWT_EXPIRATION_MINUTES(ValueType.INTEGER, "1440", "5", "10080"),
    JWT_SECRET(ValueType.BASE64_KEY, "G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=", true),
    SESSION_IDLE_TIMEOUT_MINUTES(ValueType.INTEGER, "15", "1", "480"),
    // El literal no puede ser la constante UNCONFIGURED: los campos estáticos del enum
    // se inicializan después de las constantes.
    MAILJET_API_KEY(ValueType.TEXT, "CONFIGURAR", true),
    MAILJET_API_SECRET(ValueType.TEXT, "CONFIGURAR", true),
    MAILJET_FROM_EMAIL(ValueType.EMAIL, "CONFIGURAR", false),
    MAILJET_FROM_NAME(ValueType.TEXT, "CONFIGURAR", false),
    MAILJET_API_URL(ValueType.URL, "https://api.mailjet.com/v3/send", false),
    APP_LOGIN_URL(ValueType.URL, "CONFIGURAR", false);

    /** Valor inicial de un parámetro que todavía no se ha configurado en el ambiente. */
    public static final String UNCONFIGURED = "CONFIGURAR";

    public enum ValueType {
        INTEGER, DECIMAL, TIME, FILE_EXTENSIONS, ORIGINS, TEXT, EMAIL, URL, BASE64_KEY
    }

    /** Formatos que el lector de Apache POI (WorkbookFactory) puede abrir. */
    public static final Set<String> SUPPORTED_UPLOAD_EXTENSIONS = Set.of(".xlsx", ".xls");

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final ValueType type;
    private final String defaultValue;
    private final BigDecimal min;
    private final BigDecimal max;
    private final boolean sensitive;

    SystemParameterKey(ValueType type, String defaultValue, String min, String max) {
        this(type, defaultValue, min, max, false);
    }

    SystemParameterKey(ValueType type, String defaultValue, boolean sensitive) {
        this(type, defaultValue, null, null, sensitive);
    }

    SystemParameterKey(ValueType type, String defaultValue, String min, String max, boolean sensitive) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.min = min != null ? new BigDecimal(min) : null;
        this.max = max != null ? new BigDecimal(max) : null;
        this.sensitive = sensitive;
    }

    public ValueType type() {
        return type;
    }

    public String defaultValue() {
        return defaultValue;
    }

    /** La clave o el secreto no deben quedar en los logs. */
    public boolean sensitive() {
        return sensitive;
    }

    public static Optional<SystemParameterKey> find(String key) {
        return Arrays.stream(values()).filter(k -> k.name().equals(key)).findFirst();
    }

    /**
     * Valida el valor y lo devuelve en su forma canónica.
     *
     * @throws IllegalArgumentException si el valor no es válido para este parámetro.
     */
    public String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("El valor de " + name() + " no puede estar vacío.");
        }
        String value = raw.trim();
        if (UNCONFIGURED.equals(value) && UNCONFIGURED.equals(defaultValue)) {
            return UNCONFIGURED;
        }
        return switch (type) {
            case INTEGER -> normalizeInteger(value);
            case DECIMAL -> normalizeDecimal(value);
            case TIME -> normalizeTime(value);
            case FILE_EXTENSIONS -> normalizeExtensions(value);
            case ORIGINS -> normalizeOrigins(value);
            case TEXT -> normalizeText(value);
            case EMAIL -> normalizeEmail(value);
            case URL -> normalizeUrl(value);
            case BASE64_KEY -> normalizeBase64Key(value);
        };
    }

    private String normalizeInteger(String value) {
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name() + " debe ser un número entero. Valor recibido: " + value);
        }
        checkRange(BigDecimal.valueOf(number), value);
        return String.valueOf(number);
    }

    private String normalizeDecimal(String value) {
        BigDecimal number;
        try {
            number = new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    name() + " debe ser un número decimal (use punto, por ejemplo 0.13). Valor recibido: " + value);
        }
        checkRange(number, value);
        return number.toPlainString();
    }

    private void checkRange(BigDecimal number, String value) {
        if ((min != null && number.compareTo(min) < 0) || (max != null && number.compareTo(max) > 0)) {
            throw new IllegalArgumentException(String.format("%s debe estar entre %s y %s. Valor recibido: %s",
                    name(), min.toPlainString(), max.toPlainString(), value));
        }
    }

    private String normalizeBase64Key(String value) {
        if (value.length() > 255) {
            throw new IllegalArgumentException(name() + " no puede superar 255 caracteres.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    name() + " debe ser Base64. Generar con: openssl rand -base64 32");
        }
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(name() + " debe tener al menos 256 bits (32 bytes en Base64).");
        }
        return value;
    }

    private String normalizeText(String value) {
        if (value.length() > 255) {
            throw new IllegalArgumentException(name() + " no puede superar 255 caracteres.");
        }
        return value;
    }

    private String normalizeEmail(String value) {
        try {
            return IdentifierNormalizer.requireEmail(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(name() + ": " + e.getMessage());
        }
    }

    private String normalizeUrl(String value) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : null;
            boolean validScheme = "http".equals(scheme) || "https".equals(scheme);
            boolean noQuery = uri.getRawQuery() == null && uri.getRawFragment() == null;
            String path = uri.getRawPath();
            if (validScheme && uri.getHost() != null && noQuery) {
                String normalizedPath = path == null || path.isEmpty() || "/".equals(path) ? "" : stripTrailingSlash(path);
                return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                        + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
                        + normalizedPath;
            }
        } catch (URISyntaxException e) {
            // se reporta abajo con el mismo mensaje
        }
        throw new IllegalArgumentException(
                name() + " debe ser una URL http(s), sin consulta ni fragmento. Valor recibido: " + value);
    }

    private static String stripTrailingSlash(String path) {
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private String normalizeTime(String value) {
        try {
            return LocalTime.parse(value).format(TIME_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(name() + " debe tener el formato HH:mm. Valor recibido: " + value);
        }
    }

    private String normalizeExtensions(String value) {
        Set<String> extensions = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String extension = token.trim().toLowerCase(Locale.ROOT);
            if (extension.isEmpty()) {
                continue;
            }
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            if (!SUPPORTED_UPLOAD_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException(String.format(
                        "La extensión %s no está soportada. Valores permitidos: %s.",
                        extension, String.join(", ", SUPPORTED_UPLOAD_EXTENSIONS)));
            }
            extensions.add(extension);
        }
        if (extensions.isEmpty()) {
            throw new IllegalArgumentException(name() + " debe incluir al menos una extensión.");
        }
        return String.join(",", extensions);
    }

    private String normalizeOrigins(String value) {
        Set<String> origins = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String origin = token.trim();
            if (origin.isEmpty()) {
                continue;
            }
            origins.add(normalizeOrigin(origin));
        }
        if (origins.isEmpty()) {
            throw new IllegalArgumentException(name() + " debe incluir al menos un origen.");
        }
        return String.join(",", origins);
    }

    private String normalizeOrigin(String origin) {
        String candidate = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        try {
            URI uri = new URI(candidate);
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : null;
            boolean validScheme = "http".equals(scheme) || "https".equals(scheme);
            boolean onlyOrigin = (uri.getRawPath() == null || uri.getRawPath().isEmpty())
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
            if (validScheme && uri.getHost() != null && onlyOrigin) {
                return scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT)
                        + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
            }
        } catch (URISyntaxException e) {
            // se reporta abajo con el mismo mensaje
        }
        throw new IllegalArgumentException(
                "Origen inválido: " + origin + ". Use el formato https://dominio[:puerto], sin ruta.");
    }

}
