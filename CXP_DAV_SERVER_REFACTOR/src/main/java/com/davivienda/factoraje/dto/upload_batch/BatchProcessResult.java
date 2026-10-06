package com.davivienda.factoraje.dto.upload_batch;

public record BatchProcessResult(
    byte[] pdfContent,
    boolean isSuccess
) {}