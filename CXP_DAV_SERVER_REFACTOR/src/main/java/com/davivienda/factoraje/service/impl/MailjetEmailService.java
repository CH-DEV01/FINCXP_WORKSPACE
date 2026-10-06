package com.davivienda.factoraje.service.impl;

import org.springframework.stereotype.Service;

import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.mail.MailTransport;
import com.davivienda.factoraje.infrastructure.mail.OutboundMail;
import com.davivienda.factoraje.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailjetEmailService implements EmailService {

    private final SystemParameters systemParameters;
    private final MailTransport mailTransport;

    @Override
    public boolean isConfigured() {
        return configured(SystemParameterKey.MAILJET_API_KEY)
                && configured(SystemParameterKey.MAILJET_API_SECRET)
                && configured(SystemParameterKey.MAILJET_API_URL)
                && configured(SystemParameterKey.MAILJET_FROM_EMAIL)
                && configured(SystemParameterKey.MAILJET_FROM_NAME);
    }

    @Override
    public void send(OutboundMail mail) {
        if (mail.to() == null || mail.to().isEmpty()) {
            log.warn("Correo no enviado ({}): no hay destinatarios.", mail.subject());
            return;
        }
        if (!isConfigured()) {
            log.warn("Correo no enviado ({}): Mailjet no está configurado.", mail.subject());
            return;
        }
        mailTransport.deliver(
                systemParameters.get(SystemParameterKey.MAILJET_API_KEY),
                systemParameters.get(SystemParameterKey.MAILJET_API_SECRET),
                systemParameters.get(SystemParameterKey.MAILJET_API_URL),
                systemParameters.get(SystemParameterKey.MAILJET_FROM_EMAIL),
                systemParameters.get(SystemParameterKey.MAILJET_FROM_NAME),
                mail);
    }

    private boolean configured(SystemParameterKey key) {
        String value = systemParameters.get(key);
        return value != null && !value.isBlank() && !SystemParameterKey.UNCONFIGURED.equals(value);
    }
}
