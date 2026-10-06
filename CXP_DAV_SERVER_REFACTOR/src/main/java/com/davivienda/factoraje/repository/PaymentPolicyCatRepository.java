package com.davivienda.factoraje.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;

public interface PaymentPolicyCatRepository extends JpaRepository<PaymentPolicyCat, UUID> {

    Optional<PaymentPolicyCat> findByCode(String code);

}
