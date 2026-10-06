package com.davivienda.factoraje.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.infrastructure.exception.EmailDeliveryException;
import com.mailjet.client.ClientOptions;

class MailjetTransportTest {

    @Test
    void usesTheConfiguredEndpoint() {
        ClientOptions options = MailjetTransport.clientOptions("https://api.mailjet.com/v3/send");

        assertThat(options.getBaseUrl()).isEqualTo("https://api.mailjet.com");
        assertThat(options.getVersion()).isEqualTo("v3");
    }

    @Test
    void acceptsAnotherVersionInTheSameUrl() {
        ClientOptions options = MailjetTransport.clientOptions("https://api.mailjet.com/v3.1/send");

        assertThat(options.getBaseUrl()).isEqualTo("https://api.mailjet.com");
        assertThat(options.getVersion()).isEqualTo("v3.1");
    }

    @Test
    void rejectsAnUrlThatIsNotTheSendEndpoint() {
        assertThatThrownBy(() -> MailjetTransport.clientOptions("https://api.mailjet.com/v3/contact"))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
