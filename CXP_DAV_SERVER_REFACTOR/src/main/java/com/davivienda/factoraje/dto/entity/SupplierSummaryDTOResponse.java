package com.davivienda.factoraje.dto.entity;

import java.util.UUID;

import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record SupplierSummaryDTOResponse(

        UUID id,
        String code,
        String name,
        String nit,
        GeneralStatusEnum status,
        UUID bankAccountId,
        String accountNumber

) {

    public static SupplierSummaryDTOResponse from(EntityModel supplier, BankAccountModel account) {
        return new SupplierSummaryDTOResponse(
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getNit(),
                supplier.getStatus(),
                account != null ? account.getId() : null,
                account != null ? account.getAccountNumber() : null);
    }

}
