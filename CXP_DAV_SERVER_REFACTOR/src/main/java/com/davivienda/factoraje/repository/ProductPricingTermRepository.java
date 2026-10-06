package com.davivienda.factoraje.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;

@Repository 
public interface ProductPricingTermRepository extends JpaRepository<ProductPricingTermModel, UUID>{
    
    boolean existsByCreditFacilityId(UUID creditFacilityId);

    Optional<ProductPricingTermModel> findByCreditFacilityId(UUID creditFacilityId);

    List<ProductPricingTermModel> findAllByCreditFacilityIdIn(Collection<UUID> creditFacilityIds);

}
