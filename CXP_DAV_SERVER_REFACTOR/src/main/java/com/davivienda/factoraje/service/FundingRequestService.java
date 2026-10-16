package com.davivienda.factoraje.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.funding_request.CalculateDTOResponse;

public interface FundingRequestService {

    CalculateDTOResponse checkCost(UUID masterAgreementId, List<UUID> documentIds);

    /**
     * El proveedor de la solicitud es el del convenio. La fecha y el total esperados son
     * los de la simulación que el proveedor aceptó; si al recalcular difieren, se rechaza
     * con 409 en vez de registrar condiciones distintas a las vistas.
     */
    CalculateDTOResponse submitFundingRequest(
            UUID masterAgreementId,
            UUID requestedById,
            List<UUID> documentIds,
            UUID termVersionId,
            LocalDate expectedDisbursementDate,
            BigDecimal expectedAmountToBeDisbursed,
            String userAgent);
}
