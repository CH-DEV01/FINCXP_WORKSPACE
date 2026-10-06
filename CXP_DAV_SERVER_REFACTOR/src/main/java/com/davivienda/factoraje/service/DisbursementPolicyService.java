package com.davivienda.factoraje.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.dto.disbursement_policy.DisbursementPolicyDTOResponse;

public interface DisbursementPolicyService {

    List<DisbursementPolicyDTOResponse> getDisbursementPolicies();

    /**
     * Calcula la fecha de desembolso para una solicitud recibida en
     * {@code startingDate}, según el tipo, días permitidos y offset de la
     * política, saltando fines de semana y feriados.
     */
    LocalDate calculateDisbursementDate(LocalDate startingDate, DisbursementPolicyCat disbursementPolicy);

    /**
     * Fecha de desembolso para una solicitud hecha en este momento: si ya pasó
     * la hora de corte, la solicitud se considera recibida el día siguiente.
     */
    LocalDate calculateDisbursementDateForNow(DisbursementPolicyCat disbursementPolicy);

    /** Hora a partir de la cual una solicitud se considera recibida el día siguiente. */
    LocalTime cutoffTime();

    /**
     * Verifica que la configuración de la política sea calculable.
     *
     * @throws IllegalArgumentException si la política está mal configurada.
     */
    void validatePolicy(DisbursementPolicyCat disbursementPolicy);

    /**
     * Días previos al vencimiento en los que un documento ya no puede financiarse
     * (riesgo de usura). Parámetro DUE_DATE_GRACE_DAYS.
     */
    int dueDateGraceDays();

    /** Fecha actual en la zona horaria del negocio. */
    LocalDate businessToday();

    ZoneId businessZone();

    /**
     * Límite de vencimiento para un desembolso que ocurre hoy (generación del
     * lote): un documento es financiable solo si vence después de esta fecha.
     */
    default LocalDate financeableDueDateThreshold() {
        return businessToday().plusDays(dueDateGraceDays());
    }

    /**
     * Límite de vencimiento para una solicitud hecha ahora bajo la política del
     * convenio: el documento es financiable solo si vence después de
     * (próxima fecha de desembolso + {@link #dueDateGraceDays()}).
     * Es la regla única que usan el proveedor (qué puede solicitar) y el
     * operador (qué debe ir a cuarentena / notificarse).
     */
    LocalDate financeableDueDateThreshold(DisbursementPolicyCat disbursementPolicy);

    default boolean isFinanceable(LocalDate dueDate, DisbursementPolicyCat disbursementPolicy) {
        return dueDate != null && dueDate.isAfter(financeableDueDateThreshold(disbursementPolicy));
    }

}
