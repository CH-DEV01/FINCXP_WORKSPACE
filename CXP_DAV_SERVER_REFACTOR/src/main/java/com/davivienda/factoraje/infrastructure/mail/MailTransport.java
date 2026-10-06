package com.davivienda.factoraje.infrastructure.mail;

public interface MailTransport {

    void deliver(String apiKey, String apiSecret, String apiUrl, String fromEmail, String fromName, OutboundMail mail);
}
