package com.davivienda.factoraje.infrastructure.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentifierNormalizerTest {

    @Test
    void withoutSeparatorsRemovesHyphensAndSpaces() {
        assertThat(IdentifierNormalizer.withoutSeparators("0614-010190-101-1")).isEqualTo("06140101901011");
        assertThat(IdentifierNormalizer.withoutSeparators(" 12345678-9 ")).isEqualTo("123456789");
        assertThat(IdentifierNormalizer.withoutSeparators("12\u00A034")).isEqualTo("1234");
        assertThat(IdentifierNormalizer.withoutSeparators(" - ")).isNull();
        assertThat(IdentifierNormalizer.withoutSeparators(null)).isNull();
    }

    @Test
    void withoutSeparatorsKeepsOtherCharactersForValidation() {
        assertThat(IdentifierNormalizer.withoutSeparators("12A-34")).isEqualTo("12A34");
    }

    @Test
    void caseNormalizersTrimAndTurnBlankIntoNull() {
        assertThat(IdentifierNormalizer.lowerCase("  User@Mail.COM ")).isEqualTo("user@mail.com");
        assertThat(IdentifierNormalizer.upperCase(" abc-def ")).isEqualTo("ABC-DEF");
        assertThat(IdentifierNormalizer.lowerCase("   ")).isNull();
        assertThat(IdentifierNormalizer.upperCase(null)).isNull();
    }

    @Test
    void requireNitAcceptsFourteenDigitsWithSeparators() {
        assertThat(IdentifierNormalizer.requireNit("0614-010190-101-1")).isEqualTo("06140101901011");
        assertThatThrownBy(() -> IdentifierNormalizer.requireNit("0614-0101"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("14 dígitos");
        assertThatThrownBy(() -> IdentifierNormalizer.requireNit(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El NIT es obligatorio.");
    }

    @Test
    void requireDuiAcceptsNineDigits() {
        assertThat(IdentifierNormalizer.requireDui("12345678-9")).isEqualTo("123456789");
        assertThatThrownBy(() -> IdentifierNormalizer.requireDui("1234567A-9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9 dígitos");
    }

    @Test
    void requireAccountAcceptsOnlyDigits() {
        assertThat(IdentifierNormalizer.requireAccountNumber("0012-3456")).isEqualTo("00123456");
        assertThatThrownBy(() -> IdentifierNormalizer.requireAccountNumber("ES12-3456"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requireEmailTrimsButKeepsCaseAndValidatesFormat() {
        assertThat(IdentifierNormalizer.requireEmail(" Ana@Empresa.COM ")).isEqualTo("Ana@Empresa.COM");
        assertThatThrownBy(() -> IdentifierNormalizer.requireEmail("ana@empresa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("formato válido");
        assertThatThrownBy(() -> IdentifierNormalizer.requireEmail(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El correo electrónico es obligatorio.");
    }

    @Test
    void trimmedOnlyRemovesLeadingAndTrailingWhitespace() {
        assertThat(IdentifierNormalizer.trimmed(" Ana-Lopez@Grupo-X.com ")).isEqualTo("Ana-Lopez@Grupo-X.com");
        assertThat(IdentifierNormalizer.trimmed("   ")).isNull();
        assertThat(IdentifierNormalizer.trimmed(null)).isNull();
    }

    @Test
    void paperDocumentNumberIgnoresCaseSpacesAndHyphens() {
        assertThat(IdentifierNormalizer.normalizePaperDocumentNumber("F-001")).isEqualTo("F001");
        assertThat(IdentifierNormalizer.normalizePaperDocumentNumber("f-001")).isEqualTo("F001");
        assertThat(IdentifierNormalizer.normalizePaperDocumentNumber("F 001")).isEqualTo("F001");
        assertThat(IdentifierNormalizer.normalizePaperDocumentNumber(" f\u00A0001 ")).isEqualTo("F001");
        assertThat(IdentifierNormalizer.normalizePaperDocumentNumber(" - ")).isNull();
        assertThat(IdentifierNormalizer.normalizePaperDocumentNumber(null)).isNull();
    }
}
