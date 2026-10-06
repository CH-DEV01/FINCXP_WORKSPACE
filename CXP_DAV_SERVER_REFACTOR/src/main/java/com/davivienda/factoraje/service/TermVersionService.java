package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.TermVersionCat;
import com.davivienda.factoraje.domain.enums.TermTypeUniqueCodeEnum;
import com.davivienda.factoraje.dto.term_version.TermTypeDTOResponse;
import com.davivienda.factoraje.dto.term_version.TermVersionAdminDTOResponse;
import com.davivienda.factoraje.dto.term_version.TermVersionDTORequest;
import com.davivienda.factoraje.dto.term_version.TermVersionDTOResponse;

public interface TermVersionService {

    TermVersionDTOResponse getActive(TermTypeUniqueCodeEnum termType);

    /** Versión vigente del tipo; falla si no hay ninguna publicada. */
    TermVersionCat getActiveEntity(TermTypeUniqueCodeEnum termType);

    /**
     * Devuelve la versión si está vigente y es del tipo esperado. Se usa antes de
     * registrar una aceptación para que nadie acepte un texto reemplazado.
     */
    TermVersionCat requireActiveVersion(UUID versionId, TermTypeUniqueCodeEnum termType);

    List<TermTypeDTOResponse> getTermTypes();

    List<TermVersionAdminDTOResponse> getVersions(String termTypeCode);

    TermVersionAdminDTOResponse getVersion(UUID id);

    TermVersionAdminDTOResponse createDraft(TermVersionDTORequest request);

    TermVersionAdminDTOResponse updateDraft(UUID id, TermVersionDTORequest request);

    void deleteDraft(UUID id);

    TermVersionAdminDTOResponse publish(UUID id);

}
