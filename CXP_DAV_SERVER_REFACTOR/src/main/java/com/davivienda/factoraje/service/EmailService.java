package com.davivienda.factoraje.service;

import com.davivienda.factoraje.infrastructure.mail.OutboundMail;

public interface EmailService {

    /** Falso mientras la clave, el secreto o el remitente sigan en {@code CONFIGURAR}. */
    boolean isConfigured();

    /**
     * Envía un mensaje. Si Mailjet no está configurado, no hace nada.
     *
     * @throws com.davivienda.factoraje.infrastructure.exception.EmailDeliveryException si Mailjet rechaza el envío
     */
    void send(OutboundMail mail);
}
