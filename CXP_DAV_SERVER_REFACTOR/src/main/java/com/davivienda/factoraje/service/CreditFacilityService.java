package com.davivienda.factoraje.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.davivienda.factoraje.dto.credit_facility.CreditFacilityDTOResponse;
import com.davivienda.factoraje.dto.credit_facility.RestoreCreditFacilityDTORequest;
import com.davivienda.factoraje.dto.credit_facility_history.CreditFacilityHistoryDTOResponse;

public interface CreditFacilityService {

    CreditFacilityDTOResponse restoreCreditFacility(RestoreCreditFacilityDTORequest request, String executedBy);

    CreditFacilityDTOResponse findByPayerId(UUID payerId);

    Page<CreditFacilityHistoryDTOResponse> getHistory(UUID creditFacilityId, int page, int size);
}
