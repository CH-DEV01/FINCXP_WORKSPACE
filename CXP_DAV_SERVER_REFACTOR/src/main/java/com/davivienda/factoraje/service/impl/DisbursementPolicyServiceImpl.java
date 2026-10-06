package com.davivienda.factoraje.service.impl;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.enums.DisbursementPolicyTypeEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.dto.disbursement_policy.DisbursementPolicyDTOResponse;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.repository.BankHolidayCatRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DisbursementPolicyServiceImpl implements DisbursementPolicyService {

    private static final int MAX_SEARCH_DAYS = 400;

    private final DisbursementPolicyCatRepository disbursementPolicyCatRepository;
    private final BankHolidayCatRepository bankHolidayCatRepository;
    private final SystemParameters systemParameters;
    private final ZoneId businessZone;

    public DisbursementPolicyServiceImpl(
            DisbursementPolicyCatRepository disbursementPolicyCatRepository,
            BankHolidayCatRepository bankHolidayCatRepository,
            SystemParameters systemParameters,
            @Value("${app.business-zone:America/El_Salvador}") String businessZone) {
        this.disbursementPolicyCatRepository = disbursementPolicyCatRepository;
        this.bankHolidayCatRepository = bankHolidayCatRepository;
        this.systemParameters = systemParameters;
        this.businessZone = ZoneId.of(businessZone);
    }

    @Override
    public int dueDateGraceDays() {
        return systemParameters.getInt(SystemParameterKey.DUE_DATE_GRACE_DAYS);
    }

    @Override
    public LocalDate businessToday() {
        return LocalDate.now(businessZone);
    }

    @Override
    public ZoneId businessZone() {
        return businessZone;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate calculateDisbursementDateForNow(DisbursementPolicyCat disbursementPolicy) {
        ZonedDateTime now = ZonedDateTime.now(businessZone);
        LocalTime cutoff = cutoffTime();

        LocalDate effectiveRequestDate = now.toLocalTime().isBefore(cutoff)
                ? now.toLocalDate()
                : now.toLocalDate().plusDays(1);

        return calculateDisbursementDate(effectiveRequestDate, disbursementPolicy);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate financeableDueDateThreshold(DisbursementPolicyCat disbursementPolicy) {
        if (disbursementPolicy == null) {
            return financeableDueDateThreshold();
        }
        try {
            return calculateDisbursementDateForNow(disbursementPolicy).plusDays(dueDateGraceDays());
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("No se pudo calcular la fecha de desembolso de la política {}: {}. Se usa hoy + {} días.",
                    disbursementPolicy.getCode(), e.getMessage(), dueDateGraceDays());
            return financeableDueDateThreshold();
        }
    }

    @Override
    public LocalTime cutoffTime() {
        return systemParameters.getTime(SystemParameterKey.DISBURSEMENT_CUTOFF_TIME);
    }

    @Transactional(readOnly = true)
    @Override
    public List<DisbursementPolicyDTOResponse> getDisbursementPolicies() {

        List<DisbursementPolicyCat> disbursementPolicies = disbursementPolicyCatRepository.findAll();

        return disbursementPolicies.stream()
                .map(DisbursementPolicyDTOResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate calculateDisbursementDate(LocalDate startingDate, DisbursementPolicyCat disbursementPolicy) {

        if (startingDate == null || disbursementPolicy == null) {
            throw new IllegalArgumentException("La fecha base y la política de desembolso no pueden ser nulas.");
        }

        PolicyRule rule = resolveRule(disbursementPolicy);
        Set<LocalDate> holidays = new HashSet<>(bankHolidayCatRepository.findActiveHolidayDates());

        // "T" es el primer día hábil en que se considera recibida la solicitud.
        LocalDate requestBusinessDay = nextBusinessDayInclusive(startingDate, holidays);
        LocalDate earliest = addBusinessDays(requestBusinessDay, rule.offsetDays(), holidays);

        return switch (rule.type()) {
            case T_PLUS_N -> earliest;
            case WEEKDAYS -> nextAllowedBusinessDay(earliest, rule.weekdays(), holidays, disbursementPolicy);
        };
    }

    @Override
    public void validatePolicy(DisbursementPolicyCat disbursementPolicy) {
        resolveRule(disbursementPolicy);
    }

    private record PolicyRule(DisbursementPolicyTypeEnum type, int offsetDays, Set<DayOfWeek> weekdays) {
    }

    private PolicyRule resolveRule(DisbursementPolicyCat policy) {
        String code = policy.getCode();
        DisbursementPolicyTypeEnum type = policy.getType();

        if (type == null) {
            throw new IllegalArgumentException(
                    "La política de desembolso " + code + " no tiene tipo configurado.");
        }

        Integer offset = policy.getOffsetDays();
        if (offset == null && type == DisbursementPolicyTypeEnum.T_PLUS_N) {
            throw new IllegalArgumentException(
                    "La política de desembolso " + code + " (T_PLUS_N) requiere offsetDays.");
        }
        int offsetDays = offset == null ? 0 : offset;
        if (offsetDays < 0) {
            throw new IllegalArgumentException(
                    "La política de desembolso " + code + " tiene un offsetDays negativo: " + offsetDays);
        }

        Set<DayOfWeek> weekdays = type == DisbursementPolicyTypeEnum.WEEKDAYS
                ? parseWeekdays(policy.getWeekdays(), code)
                : EnumSet.noneOf(DayOfWeek.class);

        return new PolicyRule(type, offsetDays, weekdays);
    }

    private Set<DayOfWeek> parseWeekdays(String raw, String code) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(
                    "La política de desembolso " + code + " (WEEKDAYS) requiere al menos un día en weekdays.");
        }

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String token : raw.split(",")) {
            String value = token.trim().toUpperCase(Locale.ROOT);
            if (value.isEmpty()) {
                continue;
            }
            try {
                days.add(DayOfWeek.valueOf(value));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Día inválido '" + token.trim() + "' en la política " + code
                                + ". Valores permitidos: MONDAY..SUNDAY.");
            }
        }

        days.remove(DayOfWeek.SATURDAY);
        days.remove(DayOfWeek.SUNDAY);
        if (days.isEmpty()) {
            throw new IllegalArgumentException(
                    "La política de desembolso " + code + " no tiene ningún día hábil permitido.");
        }
        return days;
    }

    private LocalDate nextBusinessDayInclusive(LocalDate date, Set<LocalDate> holidays) {
        LocalDate result = date;
        while (!isBusinessDay(result, holidays)) {
            result = result.plusDays(1);
        }
        return result;
    }

    private LocalDate addBusinessDays(LocalDate date, int businessDays, Set<LocalDate> holidays) {
        LocalDate result = date;
        for (int i = 0; i < businessDays; i++) {
            result = nextBusinessDayInclusive(result.plusDays(1), holidays);
        }
        return result;
    }

    /**
     * Si el día permitido es feriado se pasa al siguiente día permitido (no al
     * siguiente hábil) para respetar los días pactados.
     */
    private LocalDate nextAllowedBusinessDay(LocalDate from, Set<DayOfWeek> allowed, Set<LocalDate> holidays,
            DisbursementPolicyCat policy) {
        LocalDate candidate = from;
        for (int i = 0; i < MAX_SEARCH_DAYS; i++) {
            if (allowed.contains(candidate.getDayOfWeek()) && !holidays.contains(candidate)) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        throw new IllegalStateException(
                "No se encontró una fecha de desembolso válida para la política " + policy.getCode()
                        + " en los próximos " + MAX_SEARCH_DAYS + " días.");
    }

    private boolean isBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !holidays.contains(date);
    }

}
