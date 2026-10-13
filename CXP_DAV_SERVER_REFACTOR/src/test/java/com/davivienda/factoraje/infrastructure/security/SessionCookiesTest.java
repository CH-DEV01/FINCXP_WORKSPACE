package com.davivienda.factoraje.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;

class SessionCookiesTest {

    @Test
    void theCookieIsHttpOnlyAndLimitedToTheApiPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/APIFinanciamientoEmpresas");

        ResponseCookie cookie = new SessionCookies("").create("jwt", request);

        assertThat(cookie.getName()).isEqualTo(SessionCookies.NAME);
        assertThat(cookie.getValue()).isEqualTo("jwt");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/APIFinanciamientoEmpresas");
        assertThat(cookie.getMaxAge().isNegative()).isTrue();
    }

    @Test
    void theConfiguredPathReplacesTheContextPath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/interna");

        assertThat(new SessionCookies("/publica").create("jwt", request).getPath()).isEqualTo("/publica");
        assertThat(new SessionCookies("").create("jwt", new MockHttpServletRequest()).getPath()).isEqualTo("/");
    }

    @Test
    void clearingExpiresTheCookieOnTheSamePath() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/APIFinanciamientoEmpresas");

        ResponseCookie cookie = new SessionCookies("").clear(request);

        assertThat(cookie.getMaxAge().isZero()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/APIFinanciamientoEmpresas");
    }

    @Test
    void readsTheCookieFirstAndThenTheBearerHeader() {
        SessionCookies cookies = new SessionCookies("");

        MockHttpServletRequest withBoth = new MockHttpServletRequest();
        withBoth.setCookies(new Cookie(SessionCookies.NAME, "desde-cookie"));
        withBoth.addHeader(HttpHeaders.AUTHORIZATION, "Bearer desde-encabezado");
        assertThat(cookies.read(withBoth)).contains("desde-cookie");

        MockHttpServletRequest withHeader = new MockHttpServletRequest();
        withHeader.addHeader(HttpHeaders.AUTHORIZATION, "Bearer desde-encabezado");
        assertThat(cookies.read(withHeader)).contains("desde-encabezado");

        assertThat(cookies.read(new MockHttpServletRequest())).isEmpty();
    }
}
