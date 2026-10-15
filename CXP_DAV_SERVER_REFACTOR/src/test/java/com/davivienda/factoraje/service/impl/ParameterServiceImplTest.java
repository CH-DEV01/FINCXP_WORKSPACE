package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.davivienda.factoraje.domain.entities.SystemParameterModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.parameter.ParameterDTORequest;
import com.davivienda.factoraje.dto.parameter.ParameterDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.ParameterRepository;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
class ParameterServiceImplTest {

    @Mock
    private ParameterRepository parameterRepository;
    @Mock
    private SystemParameters systemParameters;
    @Mock
    private CurrentUserService currentUser;
    @InjectMocks
    private ParameterServiceImpl service;

    @Test
    void updatingASecretDoesNotWriteTheValueToTheLog() {
        UUID id = UUID.randomUUID();
        SystemParameterModel parameter = SystemParameterModel.builder()
                .id(id)
                .key("MAILJET_API_SECRET")
                .value("CONFIGURAR")
                .build();
        when(parameterRepository.findById(id)).thenReturn(Optional.of(parameter));
        when(parameterRepository.saveAndFlush(parameter)).thenReturn(parameter);

        ListAppender<ILoggingEvent> logs = attachLogs();
        service.updateParameter(id, new ParameterDTORequest("MAILJET_API_SECRET", "secreto-de-prueba"));

        assertThat(logs.list).anyMatch(event -> event.getFormattedMessage().contains("MAILJET_API_SECRET"));
        assertThat(logs.list).noneMatch(event -> event.getFormattedMessage().contains("secreto-de-prueba"));
        verify(systemParameters).invalidate();
    }

    @Test
    void readingParametersMasksSecretsButNotPendingOnes() {
        Pageable pageable = PageRequest.of(0, 10);
        when(parameterRepository.findByKeyNotIn(SystemParameterKey.businessKeys(), pageable)).thenReturn(new PageImpl<>(List.of(
                parameter("JWT_SECRET", "c2VjcmV0by1kZS1wcnVlYmEtZGUtMzItYnl0ZXMtbWluaW1v"),
                parameter("BEARER_TOKEN", "token-del-sso"),
                parameter("MAILJET_API_SECRET", "CONFIGURAR"),
                parameter("UPLOAD_MAX_ROWS", "5000"))));

        List<ParameterDTOResponse> result = service.readAllParameters(pageable).getContent();

        assertThat(result).extracting(ParameterDTOResponse::value)
                .containsExactly(ParameterDTOResponse.MASK, ParameterDTOResponse.MASK, "CONFIGURAR", "5000");
        assertThat(result).extracting(ParameterDTOResponse::sensitive)
                .containsExactly(true, true, true, false);
    }

    @Test
    void bankOperatorOnlyReadsBusinessParameters() {
        Pageable pageable = PageRequest.of(0, 10);
        when(currentUser.isAdmin()).thenReturn(true);
        when(parameterRepository.findByKeyIn(SystemParameterKey.businessKeys(), pageable))
                .thenReturn(new PageImpl<>(List.of(parameter("IVA_RATE", "0.13"))));

        List<ParameterDTOResponse> result = service.readAllParameters(pageable).getContent();

        assertThat(result).extracting(ParameterDTOResponse::key).containsExactly("IVA_RATE");
        verify(parameterRepository, never()).findByKeyNotIn(any(), any());
    }

    @Test
    void businessAndTechnicalParametersDoNotOverlap() {
        assertThat(SystemParameterKey.businessKeys()).containsExactlyInAnyOrder("IVA_RATE",
                "DEFAULT_CREDIT_THRESHOLD", "DISBURSEMENT_CUTOFF_TIME", "DUE_DATE_GRACE_DAYS", "MAX_INVOICE_AGE_DAYS");
        assertThat(SystemParameterKey.isBusiness("UPLOAD_MAX_ROWS")).isFalse();
        assertThat(SystemParameterKey.isBusiness("BEARER_TOKEN")).isFalse();
    }

    @Test
    void bankOperatorUpdatesABusinessParameter() {
        UUID id = UUID.randomUUID();
        SystemParameterModel parameter = parameter("DISBURSEMENT_CUTOFF_TIME", "15:00");
        when(currentUser.isAdmin()).thenReturn(true);
        when(parameterRepository.findById(id)).thenReturn(Optional.of(parameter));
        when(parameterRepository.saveAndFlush(parameter)).thenReturn(parameter);

        ListAppender<ILoggingEvent> logs = attachLogs();
        service.updateParameter(id, new ParameterDTORequest("DISBURSEMENT_CUTOFF_TIME", "14:30"));

        assertThat(parameter.getValue()).isEqualTo("14:30");
        assertThat(logs.list).anyMatch(event -> event.getFormattedMessage().contains("from 15:00 to 14:30"));
    }

    @Test
    void bankOperatorCannotUpdateATechnicalParameter() {
        UUID id = UUID.randomUUID();
        when(currentUser.isAdmin()).thenReturn(true);
        when(parameterRepository.findById(id)).thenReturn(Optional.of(parameter("UPLOAD_MAX_ROWS", "5000")));

        assertThatThrownBy(() -> service.updateParameter(id, new ParameterDTORequest("UPLOAD_MAX_ROWS", "100")))
                .isInstanceOf(UnauthorizedAccessException.class);
        verify(parameterRepository, never()).saveAndFlush(any());
    }

    @Test
    void systemAdminCannotUpdateABusinessParameter() {
        UUID id = UUID.randomUUID();
        when(parameterRepository.findById(id)).thenReturn(Optional.of(parameter("IVA_RATE", "0.13")));

        assertThatThrownBy(() -> service.updateParameter(id, new ParameterDTORequest("IVA_RATE", "0.15")))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("operador bancario");
        verify(parameterRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatingASecretReturnsItMasked() {
        UUID id = UUID.randomUUID();
        SystemParameterModel parameter = parameter("MAILJET_API_SECRET", "CONFIGURAR");
        when(parameterRepository.findById(id)).thenReturn(Optional.of(parameter));
        when(parameterRepository.saveAndFlush(parameter)).thenReturn(parameter);

        ParameterDTOResponse response = service.updateParameter(id,
                new ParameterDTORequest("MAILJET_API_SECRET", "secreto-de-prueba"));

        assertThat(response.value()).isEqualTo(ParameterDTOResponse.MASK);
        assertThat(parameter.getValue()).isEqualTo("secreto-de-prueba");
    }

    @Test
    void theMaskIsNotSavedAsASecret() {
        UUID id = UUID.randomUUID();
        SystemParameterModel parameter = parameter("BEARER_TOKEN", "token-del-sso");
        when(parameterRepository.findById(id)).thenReturn(Optional.of(parameter));

        assertThatThrownBy(() -> service.updateParameter(id,
                new ParameterDTORequest("BEARER_TOKEN", ParameterDTOResponse.MASK)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(parameterRepository, never()).saveAndFlush(any());
    }

    private static SystemParameterModel parameter(String key, String value) {
        return SystemParameterModel.builder().id(UUID.randomUUID()).key(key).value(value).build();
    }

    private static ListAppender<ILoggingEvent> attachLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(ParameterServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
