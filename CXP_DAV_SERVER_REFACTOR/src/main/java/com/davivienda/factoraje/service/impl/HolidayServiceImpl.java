package com.davivienda.factoraje.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.catalogs.BankHolidayCat;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.holiday.HolidayDTORequest;
import com.davivienda.factoraje.dto.holiday.HolidayDTOResponse;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.mail.AuditText;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.repository.BankHolidayCatRepository;
import com.davivienda.factoraje.service.HolidayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Los feriados activos se excluyen como días hábiles al calcular las fechas de
 * desembolso; un cambio aplica a los cálculos posteriores, no a lotes ya generados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayServiceImpl implements HolidayService {

    private final BankHolidayCatRepository bankHolidayCatRepository;
    private final MailNoticePublisher mailNotices;

    @Override
    @Transactional(readOnly = true)
    public List<HolidayDTOResponse> getHolidays() {
        return bankHolidayCatRepository.findAll(Sort.by(Sort.Direction.DESC, "holidayDate")).stream()
                .map(HolidayDTOResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public HolidayDTOResponse createHoliday(HolidayDTORequest request) {
        if (bankHolidayCatRepository.existsByHolidayDate(request.holidayDate())) {
            throw duplicateDate(request.holidayDate());
        }

        BankHolidayCat saved = bankHolidayCatRepository.saveAndFlush(BankHolidayCat.builder()
                .holidayDate(request.holidayDate())
                .description(request.description().trim())
                .status(GeneralStatusEnum.ACTIVE)
                .build());

        log.info("Holiday {} registered for {}", saved.getId(), saved.getHolidayDate());
        mailNotices.operatorChanged("Feriados", MailNoticePublisher.NO_RECORD, describe(saved));
        return HolidayDTOResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public HolidayDTOResponse updateHoliday(UUID id, HolidayDTORequest request) {
        BankHolidayCat holiday = findHoliday(id);

        if (bankHolidayCatRepository.existsByHolidayDateAndIdNot(request.holidayDate(), id)) {
            throw duplicateDate(request.holidayDate());
        }

        String previous = describe(holiday);
        holiday.setHolidayDate(request.holidayDate());
        holiday.setDescription(request.description().trim());

        BankHolidayCat saved = bankHolidayCatRepository.saveAndFlush(holiday);
        log.info("Holiday {} updated to {}", saved.getId(), saved.getHolidayDate());
        mailNotices.operatorChanged("Feriados", previous, describe(saved));
        return HolidayDTOResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteHoliday(UUID id) {
        BankHolidayCat holiday = findHoliday(id);
        bankHolidayCatRepository.delete(holiday);
        log.info("Holiday {} ({}) deleted", id, holiday.getHolidayDate());
        mailNotices.operatorChanged("Feriados", describe(holiday), "Feriado eliminado");
    }

    private static String describe(BankHolidayCat holiday) {
        return "Feriado del " + AuditText.date(holiday.getHolidayDate()) + ": " + AuditText.value(holiday.getDescription());
    }

    private BankHolidayCat findHoliday(UUID id) {
        return bankHolidayCatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el día feriado especificado."));
    }

    private static ResourceAlreadyExistsException duplicateDate(LocalDate date) {
        return new ResourceAlreadyExistsException("Ya existe un día feriado registrado para el " + date + ".");
    }
}
