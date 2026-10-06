package com.davivienda.factoraje.domain.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

class SystemParameterKeyTest {

    @Test
    void mailParametersKeepTheUnconfiguredMarkerUntilReplaced() {
        assertThat(SystemParameterKey.MAILJET_API_KEY.defaultValue()).isEqualTo(SystemParameterKey.UNCONFIGURED);
        assertThat(SystemParameterKey.MAILJET_API_SECRET.sensitive()).isTrue();
        assertThat(SystemParameterKey.MAILJET_API_KEY.sensitive()).isTrue();
        assertThat(SystemParameterKey.MAILJET_FROM_EMAIL.sensitive()).isFalse();

        assertThat(SystemParameterKey.MAILJET_FROM_EMAIL.normalize("CONFIGURAR")).isEqualTo("CONFIGURAR");
        assertThat(SystemParameterKey.APP_LOGIN_URL.normalize("CONFIGURAR")).isEqualTo("CONFIGURAR");
        assertThat(SystemParameterKey.MAILJET_FROM_EMAIL.normalize(" Aviso@Banco.COM "))
                .isEqualTo("aviso@banco.com");
        assertThat(SystemParameterKey.APP_LOGIN_URL.normalize("https://DevPay.Davivienda.com.sv/login/"))
                .isEqualTo("https://devpay.davivienda.com.sv/login");
        assertThat(SystemParameterKey.MAILJET_FROM_NAME.normalize("  Banco Davivienda  "))
                .isEqualTo("Banco Davivienda");
        assertThat(SystemParameterKey.MAILJET_API_URL.defaultValue())
                .isEqualTo("https://api.mailjet.com/v3/send");
        assertThat(SystemParameterKey.MAILJET_API_URL.normalize("https://API.Mailjet.com/v3.1/send"))
                .isEqualTo("https://api.mailjet.com/v3.1/send");
    }

    @Test
    void rejectsAnUnconfiguredMarkerOnParametersThatAlreadyHaveAValue() {
        assertThatThrownBy(() -> SystemParameterKey.IVA_RATE.normalize("CONFIGURAR"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SystemParameterKey.APP_LOGIN_URL.normalize("nota"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SystemParameterKey.MAILJET_FROM_EMAIL.normalize("sin-arroba"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void jwtSecretAcceptsOnlyABase64KeyOfAtLeast32Bytes() {
        String key = Base64.getEncoder().encodeToString(new byte[32]);

        assertThat(SystemParameterKey.JWT_SECRET.defaultValue())
                .isEqualTo("G9UuPSmEz8a18PARiE8Rs9QKvDrdUOJjjNe3duoQqUw=");
        assertThat(SystemParameterKey.JWT_SECRET.sensitive()).isTrue();
        assertThat(SystemParameterKey.JWT_SECRET.normalize(SystemParameterKey.JWT_SECRET.defaultValue()))
                .isEqualTo(SystemParameterKey.JWT_SECRET.defaultValue());
        assertThatThrownBy(() -> SystemParameterKey.JWT_SECRET.normalize("CONFIGURAR"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(SystemParameterKey.JWT_SECRET.normalize(" " + key + " ")).isEqualTo(key);
        assertThatThrownBy(() -> SystemParameterKey.JWT_SECRET.normalize("no-es-base64"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SystemParameterKey.JWT_SECRET.normalize(
                Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
