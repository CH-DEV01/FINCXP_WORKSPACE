package com.davivienda.factoraje.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;
import com.davivienda.factoraje.dto.payment_policy.PaymentPolicyDTOResponse;
import com.davivienda.factoraje.repository.PaymentPolicyCatRepository;
import com.davivienda.factoraje.service.PaymentPolicyCatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentPolicyCatServiceImpl implements PaymentPolicyCatService {

    private final PaymentPolicyCatRepository paymentPolicyCatRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaymentPolicyDTOResponse> getPaymentPolicies() {

        List<PaymentPolicyCat> paymentPolicies = paymentPolicyCatRepository.findAll();

        return paymentPolicies.stream()
                .map(PaymentPolicyDTOResponse::fromEntity)
                .toList();
    }

}
