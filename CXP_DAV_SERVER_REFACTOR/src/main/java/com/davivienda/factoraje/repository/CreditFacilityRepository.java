package com.davivienda.factoraje.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.CreditFacilityModel;

import jakarta.persistence.LockModeType;

@Repository 
public interface CreditFacilityRepository extends JpaRepository<CreditFacilityModel, UUID> {

    Optional<CreditFacilityModel> findByPayerId(UUID payerId);

    /** Toda modificación de amountInUse debe leer el cupo con este bloqueo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditFacilityModel c WHERE c.payer.id = :payerId")
    Optional<CreditFacilityModel> findByPayerIdForUpdate(@Param("payerId") UUID payerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditFacilityModel c WHERE c.id = :id")
    Optional<CreditFacilityModel> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCreditFacilityNumber(String creditFacilityNumber);

    List<CreditFacilityModel> findAllByPayerIdIn(Collection<UUID> payerIds);
    
}
