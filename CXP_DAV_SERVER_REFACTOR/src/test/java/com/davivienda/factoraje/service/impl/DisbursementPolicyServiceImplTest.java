package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.enums.DisbursementPolicyTypeEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.repository.BankHolidayCatRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;

@ExtendWith(MockitoExtension.class)
class DisbursementPolicyServiceImplTest {

    // Octubre de 2026: el jueves 1, el viernes 2, el lunes 5.
    private static final LocalDate THURSDAY = LocalDate.of(2026, 10, 1);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 10, 2);
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 3);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 10, 6);
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 7);
    private static final LocalDate NEXT_FRIDAY = LocalDate.of(2026, 10, 9);

    @Mock
    private DisbursementPolicyCatRepository disbursementPolicyCatRepository;
    @Mock
    private BankHolidayCatRepository bankHolidayCatRepository;
    @Mock
    private SystemParameters systemParameters;

    private DisbursementPolicyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DisbursementPolicyServiceImpl(
                disbursementPolicyCatRepository, bankHolidayCatRepository, systemParameters, "America/El_Salvador");
    }

    @Test
    void tPlusNCountsOnlyBusinessDays() {
        holidays();

        assertThat(service.calculateDisbursementDate(FRIDAY, tPlus(1))).isEqualTo(MONDAY);
        assertThat(service.calculateDisbursementDate(THURSDAY, tPlus(2))).isEqualTo(MONDAY);
        assertThat(service.calculateDisbursementDate(FRIDAY, tPlus(0))).isEqualTo(FRIDAY);
    }

    @Test
    void requestOnWeekendIsReceivedNextBusinessDay() {
        holidays();

        assertThat(service.calculateDisbursementDate(SATURDAY, tPlus(0))).isEqualTo(MONDAY);
        assertThat(service.calculateDisbursementDate(SATURDAY, tPlus(1))).isEqualTo(TUESDAY);
    }

    @Test
    void holidaysAreSkippedAsNonBusinessDays() {
        holidays(MONDAY);

        assertThat(service.calculateDisbursementDate(FRIDAY, tPlus(1))).isEqualTo(TUESDAY);
        assertThat(service.calculateDisbursementDate(MONDAY, tPlus(0))).isEqualTo(TUESDAY);
    }

    @Test
    void holidayOnRequestDayMovesTheStartOfTheCount() {
        holidays(FRIDAY);

        assertThat(service.calculateDisbursementDate(THURSDAY, tPlus(2))).isEqualTo(TUESDAY);
        assertThat(service.calculateDisbursementDate(FRIDAY, tPlus(1))).isEqualTo(TUESDAY);
    }

    @Test
    void weekdaysPolicyWaitsForTheNextAllowedDay() {
        holidays();

        assertThat(service.calculateDisbursementDate(MONDAY, weekdays("FRIDAY", 0))).isEqualTo(NEXT_FRIDAY);
        assertThat(service.calculateDisbursementDate(FRIDAY, weekdays("FRIDAY", 0))).isEqualTo(FRIDAY);
        assertThat(service.calculateDisbursementDate(FRIDAY, weekdays("FRIDAY", 1))).isEqualTo(NEXT_FRIDAY);
        assertThat(service.calculateDisbursementDate(TUESDAY, weekdays(" monday , wednesday ", 0)))
                .isEqualTo(WEDNESDAY);
    }

    @Test
    void weekdaysPolicyJumpsToNextAllowedDayWhenItIsAHoliday() {
        holidays(NEXT_FRIDAY);

        assertThat(service.calculateDisbursementDate(MONDAY, weekdays("FRIDAY", 0)))
                .isEqualTo(NEXT_FRIDAY.plusWeeks(1));
    }

    @Test
    void invalidPoliciesAreRejected() {
        assertThatThrownBy(() -> service.validatePolicy(policy(null, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no tiene tipo");
        assertThatThrownBy(() -> service.validatePolicy(policy(DisbursementPolicyTypeEnum.T_PLUS_N, null, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("requiere offsetDays");
        assertThatThrownBy(() -> service.validatePolicy(tPlus(-1)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("negativo");
        assertThatThrownBy(() -> service.validatePolicy(weekdays(" ", 0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("al menos un día");
        assertThatThrownBy(() -> service.validatePolicy(weekdays("MONDAY,FUNDAY", 0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Día inválido 'FUNDAY'");
        assertThatThrownBy(() -> service.validatePolicy(weekdays("SATURDAY,SUNDAY", 0)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("ningún día hábil");
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThatThrownBy(() -> service.calculateDisbursementDate(null, tPlus(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.calculateDisbursementDate(FRIDAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requestBeforeCutoffCountsFromToday() {
        holidays();
        when(systemParameters.getTime(SystemParameterKey.DISBURSEMENT_CUTOFF_TIME)).thenReturn(LocalTime.MAX);

        LocalDate today = service.businessToday();

        assertThat(service.calculateDisbursementDateForNow(tPlus(0)))
                .isEqualTo(service.calculateDisbursementDate(today, tPlus(0)));
    }

    @Test
    void requestAfterCutoffCountsFromTomorrow() {
        holidays();
        when(systemParameters.getTime(SystemParameterKey.DISBURSEMENT_CUTOFF_TIME)).thenReturn(LocalTime.MIDNIGHT);

        LocalDate today = service.businessToday();

        assertThat(service.calculateDisbursementDateForNow(tPlus(0)))
                .isEqualTo(service.calculateDisbursementDate(today.plusDays(1), tPlus(0)));
    }

    private void holidays(LocalDate... dates) {
        when(bankHolidayCatRepository.findActiveHolidayDates()).thenReturn(List.of(dates));
    }

    private static DisbursementPolicyCat tPlus(int offsetDays) {
        return policy(DisbursementPolicyTypeEnum.T_PLUS_N, offsetDays, null);
    }

    private static DisbursementPolicyCat weekdays(String days, int offsetDays) {
        return policy(DisbursementPolicyTypeEnum.WEEKDAYS, offsetDays, days);
    }

    private static DisbursementPolicyCat policy(DisbursementPolicyTypeEnum type, Integer offsetDays, String days) {
        return DisbursementPolicyCat.builder()
                .code("TEST")
                .type(type)
                .offsetDays(offsetDays)
                .weekdays(days)
                .build();
    }
}
