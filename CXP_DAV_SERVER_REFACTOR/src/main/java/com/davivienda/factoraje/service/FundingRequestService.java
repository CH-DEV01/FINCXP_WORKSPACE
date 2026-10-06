package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.funding_request.CalculateDTOResponse;

public interface FundingRequestService {

    CalculateDTOResponse checkCost(UUID masterAgreementId, List<UUID> documentIds);

    /** El proveedor de la solicitud es el del convenio. */
    CalculateDTOResponse submitFundingRequest(
            UUID masterAgreementId,
            UUID requestedById,
            List<UUID> documentIds,
            UUID termVersionId,
            String userAgent);
}
