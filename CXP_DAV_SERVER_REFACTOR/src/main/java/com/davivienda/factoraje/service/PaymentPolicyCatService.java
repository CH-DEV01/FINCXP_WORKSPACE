package com.davivienda.factoraje.service;

import java.util.List;

import com.davivienda.factoraje.dto.payment_policy.PaymentPolicyDTOResponse;

public interface PaymentPolicyCatService {

    List<PaymentPolicyDTOResponse> getPaymentPolicies();
    
}
