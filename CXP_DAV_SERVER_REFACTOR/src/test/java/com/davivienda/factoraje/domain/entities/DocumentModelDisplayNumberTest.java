package com.davivienda.factoraje.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentModelDisplayNumberTest {

    @Test
    void prefersTheDocumentNumberWhenPresent() {
        DocumentModel doc = DocumentModel.builder()
                .documentNumber("FAC-001")
                .controlNumber("DTE-CTRL-9")
                .build();

        assertThat(doc.displayNumber()).isEqualTo("FAC-001");
    }

    @Test
    void fallsBackToTheControlNumberWhenTheDocumentNumberIsMissing() {
        DocumentModel blank = DocumentModel.builder()
                .documentNumber("  ")
                .controlNumber("DTE-CTRL-9")
                .build();
        DocumentModel missing = DocumentModel.builder()
                .documentNumber(null)
                .controlNumber("DTE-CTRL-9")
                .build();

        assertThat(blank.displayNumber()).isEqualTo("DTE-CTRL-9");
        assertThat(missing.displayNumber()).isEqualTo("DTE-CTRL-9");
    }
}
