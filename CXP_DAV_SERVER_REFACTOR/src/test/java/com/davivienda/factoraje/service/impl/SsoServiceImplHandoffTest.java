package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.davivienda.factoraje.domain.entities.SystemParameterModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.sso.HandoffDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.BusinessException;
import com.davivienda.factoraje.infrastructure.security.JwtService;
import com.davivienda.factoraje.repository.ParameterRepository;
import com.davivienda.factoraje.repository.PendingSessionRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.PendingSessionService;

class SsoServiceImplHandoffTest {

    private static final String PAY_URL = "https://pay.davivienda.com.sv/otc/validate";

    private final ParameterRepository parameterRepository = mock(ParameterRepository.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final MockRestServiceServer payDavivienda = MockRestServiceServer.createServer(restTemplate);
    private final PendingSessionRepository pendingSessionRepository = mock(PendingSessionRepository.class);
    private final SsoServiceImpl service = new SsoServiceImpl(parameterRepository, restTemplate,
            pendingSessionRepository, mock(PendingSessionService.class), mock(UserRepository.class),
            mock(JwtService.class));

    @Test
    void anUnconfiguredIntegrationRejectsTheDefaultValues() {
        String unconfigured = SystemParameterKey.UNCONFIGURED;
        parameter("BEARER_TOKEN", unconfigured);
        parameter("PAY_BEARER_TOKEN", unconfigured);
        parameter("APP_CODE", unconfigured);
        parameter("PAY_DAVIVIENDA_URL", unconfigured);

        assertThatThrownBy(() -> service.processHandoff(unconfigured, "otc", "Bearer " + unconfigured))
                .isInstanceOf(BusinessException.class)
                .hasMessage("La integración SSO no está configurada");
        verifyNoInteractions(pendingSessionRepository);
        payDavivienda.verify();
    }

    @Test
    void theOtcIsSerializedSoQuotesCannotAlterTheJson() {
        configured();
        payDavivienda.expect(requestTo(PAY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Authorization", "Bearer pay-secret"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"otc\":\"abc\\\",\\\"user\\\":\\\"admin\"}"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"status\":\"validated\",\"user\":\"ana@empresa.com\"}}",
                        MediaType.APPLICATION_JSON));

        HandoffDTOResponse response = service.processHandoff("FINCXP", "abc\",\"user\":\"admin", "Bearer my-secret");

        assertThat(response.getSuccess()).isTrue();
        verify(pendingSessionRepository).save(any());
        payDavivienda.verify();
    }

    @Test
    void anOtcWithControlCharactersIsRejectedBeforeCallingDavivienda() {
        configured();

        assertThatThrownBy(() -> service.processHandoff("FINCXP", "abc\n", "Bearer my-secret"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("OTC con formato inválido");
        payDavivienda.verify();
    }

    private void configured() {
        parameter("BEARER_TOKEN", "my-secret");
        parameter("PAY_BEARER_TOKEN", "pay-secret");
        parameter("APP_CODE", "FINCXP");
        parameter("PAY_DAVIVIENDA_URL", PAY_URL);
    }

    private void parameter(String key, String value) {
        SystemParameterModel parameter = new SystemParameterModel();
        parameter.setKey(key);
        parameter.setValue(value);
        when(parameterRepository.findByKey(key)).thenReturn(Optional.of(parameter));
    }
}
