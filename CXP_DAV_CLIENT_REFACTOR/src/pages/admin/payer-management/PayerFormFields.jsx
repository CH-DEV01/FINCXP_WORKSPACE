import React, { useMemo } from "react";

import FieldLabel from "../../../components/form/FieldLabel";
import FieldError from "../../../components/form/FieldError";
import CurrencyInput from "../../../components/form/CurrencyInput";
import inputClass from "../../../components/form/inputClass";
import { STATUS_OPTIONS } from "../../../constants/status";
import { formatCurrency } from "../../../utils/format";
import { CALCULATION_BASE_OPTIONS, formatNIT } from "./payerForm";

const SectionTitle = ({ children }) => (
  <h3 className="md:col-span-2 text-xs font-bold text-gray-700 border-b border-gray-200 pb-2 mt-2">{children}</h3>
);

const PayerFormFields = ({ form, isEditing, canEditAccount, onSubmit }) => {
  const {
    register,
    handleSubmit,
    watch,
    control,
    formState: { errors },
  } = form;

  const watchApproved = watch("approvedAmount", "");
  const watchConsumed = watch("consumedAmount", "");
  const watchThreshold = watch("warningThreshold", "");
  const hasInitialConsumption = Number(watchConsumed) > 0;

  // La carga de documentos se bloquea al superar monto aprobado × umbral
  const loadCapacity = useMemo(() => {
    const approved = Number(watchApproved || 0);
    const consumed = Number(watchConsumed || 0);
    const threshold = Number(watchThreshold || 0);
    if (!(approved > 0) || !(consumed > 0) || !(threshold > 0)) return null;

    const thresholdLimit = approved * (threshold / 100);
    return {
      available: Math.max(thresholdLimit - consumed, 0),
      blocked: consumed >= thresholdLimit,
    };
  }, [watchApproved, watchConsumed, watchThreshold]);

  return (
    <form className="grid grid-cols-1 md:grid-cols-2 gap-4" onSubmit={handleSubmit(onSubmit)}>
      {isEditing && (
        <p className="md:col-span-2 rounded-lg border border-gray-200 bg-gray-50 px-3 py-2 text-[11px] text-gray-600">
          Solo se pueden modificar la cuenta bancaria, las tasas, la base de cálculo, el estado y el umbral. El resto de
          datos del pagador y del cupo se muestran como referencia.
        </p>
      )}

      <SectionTitle>Datos del pagador</SectionTitle>

      <div>
        <FieldLabel htmlFor="payer-name">Nombre</FieldLabel>
        <input
          id="payer-name"
          type="text"
          maxLength={255}
          readOnly={isEditing}
          {...register("name")}
          className={inputClass(errors.name, isEditing)}
          placeholder="Nombre del pagador"
        />
        <FieldError error={errors.name} />
      </div>

      <div>
        <FieldLabel htmlFor="payer-nit">NIT</FieldLabel>
        <input
          id="payer-nit"
          type="text"
          maxLength={17}
          readOnly={isEditing}
          {...register("nit", {
            onChange: (e) => {
              e.target.value = formatNIT(e.target.value);
            },
          })}
          className={inputClass(errors.nit, isEditing)}
          placeholder="0000-000000-000-0"
        />
        <FieldError error={errors.nit} />
      </div>

      {isEditing && (
        <div>
          <FieldLabel htmlFor="payer-status">Estado</FieldLabel>
          <select id="payer-status" {...register("status")} className={inputClass(errors.status)}>
            {STATUS_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
          <FieldError error={errors.status} />
        </div>
      )}

      <div className={isEditing ? "" : "md:col-span-2"}>
        <FieldLabel htmlFor="payer-account-number">Cuenta bancaria</FieldLabel>
        <input
          id="payer-account-number"
          type="text"
          inputMode="numeric"
          maxLength={50}
          readOnly={!canEditAccount}
          {...register("accountNumber", {
            onChange: (e) => {
              e.target.value = e.target.value.replace(/\D/g, "");
            },
          })}
          className={inputClass(errors.accountNumber, !canEditAccount)}
          placeholder={canEditAccount ? "Ej. 000123456789" : "Sin cuenta registrada"}
        />
        <FieldError error={errors.accountNumber} />
      </div>

      <SectionTitle>Cupo de crédito</SectionTitle>

      <div>
        <FieldLabel htmlFor="payer-credit-facility-number">Número de cupo</FieldLabel>
        <input
          id="payer-credit-facility-number"
          type="text"
          maxLength={255}
          readOnly={isEditing}
          {...register("creditFacilityNumber")}
          className={inputClass(errors.creditFacilityNumber, isEditing)}
          placeholder="Ej. CF-0001"
        />
        <FieldError error={errors.creditFacilityNumber} />
      </div>

      <div>
        <FieldLabel htmlFor="payer-approved-amount">Monto aprobado</FieldLabel>
        <CurrencyInput
          id="payer-approved-amount"
          name="approvedAmount"
          control={control}
          hasError={errors.approvedAmount}
          locked={isEditing}
        />
        <FieldError error={errors.approvedAmount} />
      </div>

      <div className="md:col-span-2">
        <FieldLabel htmlFor="payer-warning-threshold">Umbral de alerta (%)</FieldLabel>
        <input
          id="payer-warning-threshold"
          type="number"
          step="1"
          min="1"
          max="100"
          {...register("warningThreshold")}
          className={inputClass(errors.warningThreshold)}
          placeholder="Ej. 80"
        />
        <p className="text-gray-400 text-[10px] mt-1">
          Porcentaje de ocupación del cupo a partir del cual el estado pasa a crítico.
        </p>
        <FieldError error={errors.warningThreshold} />
      </div>

      <div className={isEditing ? "md:col-span-2" : ""}>
        <FieldLabel htmlFor="payer-consumed-amount">
          {isEditing ? "Monto consumido" : "Monto consumido (opcional)"}
        </FieldLabel>
        <CurrencyInput
          id="payer-consumed-amount"
          name="consumedAmount"
          control={control}
          hasError={errors.consumedAmount}
          locked={isEditing}
        />
        <p className="text-gray-400 text-[10px] mt-1">
          {isEditing ? "Consumo actual del cupo." : "Saldo ya utilizado del cupo antes de registrarlo en el sistema."}
        </p>
        <FieldError error={errors.consumedAmount} />
      </div>

      {!isEditing && (
        <div>
          <FieldLabel htmlFor="payer-consumption-reference">Referencia del consumo</FieldLabel>
          <input
            id="payer-consumption-reference"
            type="text"
            maxLength={255}
            readOnly={!hasInitialConsumption}
            {...register("consumptionReference")}
            className={inputClass(errors.consumptionReference, !hasInitialConsumption)}
            placeholder="Ej. N° de operación en el core"
          />
          <FieldError error={errors.consumptionReference} />
        </div>
      )}

      {loadCapacity && (
        <div
          className={`md:col-span-2 rounded-lg border px-3 py-2 text-[11px] ${
            loadCapacity.blocked
              ? "border-red-200 bg-red-50 text-red-700"
              : "border-amber-200 bg-amber-50 text-amber-800"
          }`}
        >
          {loadCapacity.blocked
            ? "Con este consumo el pagador supera el umbral y no podrá cargar documentos hasta que se registren abonos al cupo."
            : `Disponible para carga de documentos según el umbral: ${formatCurrency(loadCapacity.available)}.`}
        </div>
      )}

      <SectionTitle>Condiciones financieras</SectionTitle>

      <div>
        <FieldLabel htmlFor="payer-interest-rate">Tasa de interés (%)</FieldLabel>
        <input
          id="payer-interest-rate"
          type="number"
          step="0.0001"
          min="0"
          max="100"
          {...register("interestRate")}
          className={inputClass(errors.interestRate)}
          placeholder="Ej. 15.5"
        />
        <FieldError error={errors.interestRate} />
      </div>

      <div>
        <FieldLabel htmlFor="payer-commission-rate">Tasa de comisión (%)</FieldLabel>
        <input
          id="payer-commission-rate"
          type="number"
          step="0.0001"
          min="0"
          max="100"
          {...register("commissionRate")}
          className={inputClass(errors.commissionRate)}
          placeholder="Ej. 1.5"
        />
        <FieldError error={errors.commissionRate} />
      </div>

      <div className="md:col-span-2">
        <FieldLabel htmlFor="payer-calculation-base">Base de cálculo</FieldLabel>
        <select
          id="payer-calculation-base"
          {...register("calculationBase")}
          className={inputClass(errors.calculationBase)}
        >
          {CALCULATION_BASE_OPTIONS.map(({ value, label }) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
        <FieldError error={errors.calculationBase} />
      </div>
    </form>
  );
};

export default PayerFormFields;
