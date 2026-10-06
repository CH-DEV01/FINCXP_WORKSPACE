package com.davivienda.factoraje.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.BankAccountModel;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccountModel, UUID> {

    Optional<BankAccountModel> findFirstByEntityModelIdAndIsMainTrue(UUID entityId);

    List<BankAccountModel> findAllByEntityModelIdInAndIsMainTrue(Collection<UUID> entityIds);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByAccountNumberAndIdNot(String accountNumber, UUID id);

    List<BankAccountModel> findAllByAccountNumberIn(Collection<String> accountNumbers);

}
