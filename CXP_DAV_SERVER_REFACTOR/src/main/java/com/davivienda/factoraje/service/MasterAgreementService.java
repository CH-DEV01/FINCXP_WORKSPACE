package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;

import com.davivienda.factoraje.dto.disbursement_policy.NextDisbursementDateDTOResponse;
import com.davivienda.factoraje.dto.master_agreement.MasterAgreementDTORequest;
import com.davivienda.factoraje.dto.master_agreement.MasterAgreementDTOResponse;

public interface MasterAgreementService {
    
    MasterAgreementDTOResponse createMasterAgreement(MasterAgreementDTORequest request);

    /** Con {@code payerId}, solo los convenios de ese pagador y la búsqueda es por nombre del proveedor. */
    Page<MasterAgreementDTOResponse> getPaginatedAgreements(int page, int size, String search, UUID payerId);

    MasterAgreementDTOResponse updateMasterAgreement(UUID id, MasterAgreementDTORequest request);
    
    /** Con {@code onlyActive} omite convenios inactivos o de pagadores inactivos. */
    List<MasterAgreementDTOResponse> getMasterAgreementsBySupplier(UUID supplierId, boolean onlyActive);

    NextDisbursementDateDTOResponse getNextDisbursementDate(UUID masterAgreementId);
}
