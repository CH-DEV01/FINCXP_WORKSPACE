package com.davivienda.factoraje.infrastructure.mail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class MailTemplateRenderer {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    private static final Pattern CONDITIONAL = Pattern.compile("<!--if:(\\w+)-->(.*?)<!--endif:\\1-->", Pattern.DOTALL);

    /**
     * Reemplaza {{clave}} por su valor escapado. Un bloque {@code <!--if:clave-->...<!--endif:clave-->}
     * se conserva solo si la clave tiene un valor no vacío.
     */
    public String render(String template, Map<String, String> values) {
        String html = cache.computeIfAbsent(template, this::load);
        String rendered = CONDITIONAL.matcher(html).replaceAll(match -> {
            String value = values.get(match.group(1));
            return value != null && !value.isBlank() ? Matcher.quoteReplacement(match.group(2)) : "";
        });
        for (Map.Entry<String, String> entry : values.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", escape(entry.getValue()));
        }
        return rendered;
    }

    private String load(String template) {
        ClassPathResource resource = new ClassPathResource("mail/" + template);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se encontró la plantilla de correo " + template + ".", e);
        }
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
