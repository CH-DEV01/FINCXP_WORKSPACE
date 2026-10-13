package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SsoServiceImplSecretEqualsTest {

    @Test
    void acceptsOnlyTheExactSecret() {
        assertThat(SsoServiceImpl.secretEquals("Bearer abc123", "Bearer abc123")).isTrue();
        assertThat(SsoServiceImpl.secretEquals("Bearer abc123", "Bearer abc124")).isFalse();
        assertThat(SsoServiceImpl.secretEquals("Bearer abc123", "Bearer abc1234")).isFalse();
        assertThat(SsoServiceImpl.secretEquals("Bearer abc123", "")).isFalse();
    }

    @Test
    void aMissingValueNeverMatches() {
        assertThat(SsoServiceImpl.secretEquals("Bearer abc123", null)).isFalse();
        assertThat(SsoServiceImpl.secretEquals(null, null)).isFalse();
    }
}
