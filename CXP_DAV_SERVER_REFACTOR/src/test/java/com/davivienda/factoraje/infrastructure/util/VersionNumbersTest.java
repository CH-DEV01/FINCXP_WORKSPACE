package com.davivienda.factoraje.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VersionNumbersTest {

    @Test
    void equivalentVersionSpellingsNormalizeToTheSameValue() {
        assertThat(VersionNumbers.normalize("1.0")).isEqualTo("1.0");
        assertThat(VersionNumbers.normalize("1.00")).isEqualTo("1.0");
        assertThat(VersionNumbers.normalize("01.0")).isEqualTo("1.0");
        assertThat(VersionNumbers.normalize(" 01.00 ")).isEqualTo("1.0");
    }

    @Test
    void distinctVersionsStayDistinct() {
        assertThat(VersionNumbers.normalize("1.1")).isEqualTo("1.1");
        assertThat(VersionNumbers.normalize("1.10")).isEqualTo("1.10");
        assertThat(VersionNumbers.normalize("1.1.0")).isEqualTo("1.1.0");
    }
}
