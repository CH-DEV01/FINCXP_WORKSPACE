package com.davivienda.factoraje.dto.dispersion;

import java.util.List;

/** Lote de dispersión con sus documentos y el usuario del pagador que firma la carta. */
public record DispersionBatchDetailDTOResponse(
        DispersionBatchSummaryDTOResponse batch,
        String signerName,
        List<DispersionDocumentDTOResponse> documents
) {}
