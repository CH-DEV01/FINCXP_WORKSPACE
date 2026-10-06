package com.davivienda.factoraje.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.dto.supplier.SupplierBankAccountDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.service.SupplierBankAccountService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierBankAccountServiceImpl implements SupplierBankAccountService {

    private final CurrentUserService currentUser;
    private final BankAccountRepository bankAccountRepository;

    /** Misma cuenta que usan la solicitud y la carta de desembolso: la principal de la entidad. */
    @Override
    @Transactional(readOnly = true)
    public SupplierBankAccountDTOResponse getOwnMainAccount() {
        if (currentUser.get().getEntity() == null) {
            throw new UnauthorizedAccessException("El usuario no está asociado a un proveedor.");
        }
        String accountNumber = bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(currentUser.entityId())
                .map(BankAccountModel::getAccountNumber)
                .orElse(null);
        return new SupplierBankAccountDTOResponse(accountNumber);
    }
}
