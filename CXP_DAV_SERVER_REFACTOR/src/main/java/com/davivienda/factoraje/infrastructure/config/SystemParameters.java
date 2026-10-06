package com.davivienda.factoraje.infrastructure.config;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.repository.ParameterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Lectura tipada de la tabla system_parameters. Los valores se guardan en
 * memoria por un minuto para no consultar la base en cada petición (CORS y JWT
 * se leen en cada request); al editar un parámetro la caché se limpia, así que
 * en esta instancia el cambio aplica de inmediato y en las demás en menos de un
 * minuto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemParameters {

    private static final long TTL_MILLIS = 60_000;

    private final ParameterRepository parameterRepository;
    private final Map<SystemParameterKey, CachedValue> cache = new ConcurrentHashMap<>();

    private record CachedValue(String value, long loadedAt) {
    }

    public String get(SystemParameterKey key) {
        long now = System.currentTimeMillis();
        CachedValue cached = cache.get(key);
        if (cached != null && now - cached.loadedAt() < TTL_MILLIS) {
            return cached.value();
        }
        try {
            String value = load(key);
            cache.put(key, new CachedValue(value, now));
            return value;
        } catch (DataAccessException e) {
            String fallback = cached != null ? cached.value() : key.defaultValue();
            if (key.sensitive()) {
                log.error("No se pudo leer el parámetro {} de la base de datos. Se usa el valor disponible.", key, e);
            } else {
                log.error("No se pudo leer el parámetro {} de la base de datos. Se usa {}.", key, fallback, e);
            }
            return fallback;
        }
    }

    public int getInt(SystemParameterKey key) {
        return Integer.parseInt(get(key));
    }

    public BigDecimal getDecimal(SystemParameterKey key) {
        return new BigDecimal(get(key));
    }

    public LocalTime getTime(SystemParameterKey key) {
        return LocalTime.parse(get(key));
    }

    public List<String> getList(SystemParameterKey key) {
        return Arrays.stream(get(key).split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void invalidate() {
        cache.clear();
    }

    private String load(SystemParameterKey key) {
        return parameterRepository.findByKey(key.name())
                .map(parameter -> {
                    try {
                        return key.normalize(parameter.getValue());
                    } catch (IllegalArgumentException e) {
                        if (key.sensitive()) {
                            log.error("Valor inválido en el parámetro {}. Se usa el valor por defecto.", key);
                        } else {
                            log.error("Valor inválido en el parámetro {}: {}. Se usa el valor por defecto {}.",
                                    key, e.getMessage(), key.defaultValue());
                        }
                        return key.defaultValue();
                    }
                })
                .orElseGet(() -> {
                    if (key.sensitive()) {
                        log.warn("No existe el parámetro {}. Se usa el valor por defecto.", key);
                    } else {
                        log.warn("No existe el parámetro {}. Se usa el valor por defecto {}.", key, key.defaultValue());
                    }
                    return key.defaultValue();
                });
    }

}
