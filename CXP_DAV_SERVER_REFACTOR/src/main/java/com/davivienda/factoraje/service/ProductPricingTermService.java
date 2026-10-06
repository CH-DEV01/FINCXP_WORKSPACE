package com.davivienda.factoraje.service;

import com.davivienda.factoraje.dto.product_pricing_term.ProductPricingTermDTORequest;
import com.davivienda.factoraje.dto.product_pricing_term.ProductPricingTermDTOResponse;

public interface ProductPricingTermService {

    ProductPricingTermDTOResponse createProductPricingTerm(ProductPricingTermDTORequest request);
    
}
