package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.davivienda.factoraje.domain.entities.SystemParameterModel;
import com.davivienda.factoraje.dto.parameter.ParameterDTORequest;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
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

    private static ListAppender<ILoggingEvent> attachLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(ParameterServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }
}
