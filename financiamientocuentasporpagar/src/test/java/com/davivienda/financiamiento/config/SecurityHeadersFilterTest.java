package com.davivienda.financiamiento.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.davivienda.financiamiento.domain.entities.SystemParameterModel;
import com.davivienda.financiamiento.repository.ParameterRepository;

class SecurityHeadersFilterTest {

    private static final Instant START = Instant.parse("2026-10-14T12:00:00Z");

    private ParameterRepository repository;
    private Clock clock;
    private SecurityHeadersFilter filter;

    @BeforeEach
    void setUp() {
        repository = mock(ParameterRepository.class);
        when(repository.findByKey(anyString())).thenReturn(Optional.empty());
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(START);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        filter = new SecurityHeadersFilter(repository, clock);
    }

    @Test
    void withoutParametersReportsOnlyAndAllowsAnyHttpsApi() throws Exception {
        MockHttpServletResponse response = run();

        assertThat(response.getHeader(SecurityHeadersFilter.ENFORCE_HEADER)).isNull();
        assertThat(response.getHeader(SecurityHeadersFilter.REPORT_ONLY_HEADER))
                .contains("script-src 'self';")
                .contains("connect-src 'self' https:;")
                .contains("frame-ancestors 'self' https://*.davivienda.com.sv")
                .contains("object-src 'none'");
    }

    @Test
    void enforceModeUsesConfiguredCoreApiOrigins() throws Exception {
        parameter("CSP_MODE", " enforce ");
        parameter("CSP_CONNECT_SRC",
                "https://dev-financiamientocuentasporpagar-api.davivienda.com.sv:8443/,https://api.davivienda.com.sv");

        MockHttpServletResponse response = run();

        assertThat(response.getHeader(SecurityHeadersFilter.REPORT_ONLY_HEADER)).isNull();
        assertThat(response.getHeader(SecurityHeadersFilter.ENFORCE_HEADER)).contains(
                "connect-src 'self' https://dev-financiamientocuentasporpagar-api.davivienda.com.sv:8443 https://api.davivienda.com.sv;");
    }

    @Test
    void disabledModeSendsNoPolicyButKeepsTheOtherHeaders() throws Exception {
        parameter("CSP_MODE", "DISABLED");

        MockHttpServletResponse response = run();

        assertThat(response.getHeader(SecurityHeadersFilter.ENFORCE_HEADER)).isNull();
        assertThat(response.getHeader(SecurityHeadersFilter.REPORT_ONLY_HEADER)).isNull();
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
    }

    @Test
    void unknownModeFallsBackToReportOnly() throws Exception {
        parameter("CSP_MODE", "BLOQUEAR");

        assertThat(run().getHeader(SecurityHeadersFilter.REPORT_ONLY_HEADER)).isNotNull();
    }

    @Test
    void invalidOriginsCannotInjectDirectives() throws Exception {
        parameter("CSP_MODE", "ENFORCE");
        parameter("CSP_CONNECT_SRC", "https://api.davivienda.com.sv; script-src *");

        String policy = run().getHeader(SecurityHeadersFilter.ENFORCE_HEADER);

        assertThat(policy).doesNotContain("script-src *").contains("connect-src 'self' https:;");
    }

    @Test
    void unconfiguredOriginsUseTheDefault() throws Exception {
        parameter("CSP_CONNECT_SRC", "CONFIGURAR");

        assertThat(run().getHeader(SecurityHeadersFilter.REPORT_ONLY_HEADER))
                .contains("connect-src 'self' https:;");
    }

    @Test
    void databaseFailureStillServesWithReportOnlyPolicy() throws Exception {
        when(repository.findByKey(anyString())).thenThrow(new IllegalStateException("sin conexión"));

        assertThat(run().getHeader(SecurityHeadersFilter.REPORT_ONLY_HEADER))
                .contains("connect-src 'self' https:;");
    }

    @Test
    void parametersAreCachedForSixtySeconds() throws Exception {
        run();
        when(clock.instant()).thenReturn(START.plusSeconds(59));
        run();
        verify(repository, times(1)).findByKey("CSP_MODE");

        when(clock.instant()).thenReturn(START.plus(SecurityHeadersFilter.CACHE_TTL));
        run();
        verify(repository, times(2)).findByKey("CSP_MODE");
    }

    @Test
    void frontendRoutesServedThroughTheErrorPageAlsoGetThePolicy() {
        assertThat(filter.shouldNotFilterErrorDispatch()).isFalse();
    }

    private void parameter(String key, String value) {
        when(repository.findByKey(key)).thenReturn(Optional.of(
                SystemParameterModel.builder().key(key).value(value).build()));
    }

    private MockHttpServletResponse run() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/financiamientocuentasporpagar/"), response,
                new MockFilterChain());
        return response;
    }
}
