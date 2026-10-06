package com.davivienda.factoraje.infrastructure.mail;

import java.util.List;

public record OutboundMail(
        List<MailRecipient> to,
        List<MailRecipient> cc,
        String subject,
        String html,
        String text) {
}
