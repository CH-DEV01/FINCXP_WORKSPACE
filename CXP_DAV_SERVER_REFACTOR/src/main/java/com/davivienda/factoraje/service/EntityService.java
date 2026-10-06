package com.davivienda.factoraje.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.dto.entity.EntityDTORequest;
import com.davivienda.factoraje.dto.entity.EntityDTOResponse;
import com.davivienda.factoraje.dto.entity.PayerRegistrationDTORequest;
import com.davivienda.factoraje.dto.entity.PayerSummaryDTOResponse;
import com.davivienda.factoraje.dto.entity.PayerUpdateDTORequest;
import com.davivienda.factoraje.dto.entity.SupplierSummaryDTOResponse;
import com.davivienda.factoraje.dto.entity.SupplierUpdateDTORequest;

public interface EntityService {

    EntityDTOResponse createEntity(EntityDTORequest request);

    /** Proveedor nuevo desde la carga de documentos, identificado por su NIT. */
    EntityModel createSupplier(String nit, String name);

    PayerSummaryDTOResponse registerPayer(PayerRegistrationDTORequest request);

    PayerSummaryDTOResponse updatePayer(UUID payerId, PayerUpdateDTORequest request);

    Page<PayerSummaryDTOResponse> getPayerSummaries(String search, int page, int size);

    Page<SupplierSummaryDTOResponse> getSupplierSummaries(String search, int page, int size);

    SupplierSummaryDTOResponse updateSupplier(UUID supplierId, SupplierUpdateDTORequest request);

    Page<EntityDTOResponse> getPayers(Pageable pageable);

    Page<EntityDTOResponse> getSuppliers(Pageable pageable);

    Page<EntityDTOResponse> getEntities(Pageable pageable);

    /** Entidades del tipo que exige el rol, para asignarlas a un usuario. Busca por nombre o NIT. */
    Page<EntityDTOResponse> searchEntitiesForRole(UUID roleId, String search, int page, int size);

}
