import React from "react";
import Icon from "@mdi/react";
import { mdiCreditCardSettings, mdiCashRegister, mdiPercentOutline, mdiReceiptTextOutline } from "@mdi/js";
import { formatMoney, formatPercent, formatRate } from "../../../utils/format";
import { CALCULATION_BASE_LABELS, THRESHOLD_STATUS } from "./creditLineConfig";

const CreditLineDetailCard = ({ data, onRegisterRepayment }) => {
  const isCritical = data.thresholdStatus === "CRITICAL";
  const thresholdStatus = THRESHOLD_STATUS[data.thresholdStatus] || THRESHOLD_STATUS.NORMAL;

  const pricingItems = [
    {
      key: "interest",
      icon: mdiPercentOutline,
      label: "Tasa de interés",
      value: formatRate(data.interestRate),
      hint:
        data.interestRate != null
          ? CALCULATION_BASE_LABELS[data.calculationBase] || "Base no definida"
          : "Sin condición de precio configurada",
    },
    {
      key: "commission",
      icon: mdiReceiptTextOutline,
      label: "Tasa de comisión",
      value: formatRate(data.commissionRate),
      hint: data.commissionRate != null ? null : "Sin condición de precio configurada",
    },
  ];

  const metrics = [
    {
      key: "total",
      label: "Cupo total autorizado",
      value: data.totalLimit,
      labelClass: "text-slate-400",
      valueClass: "text-slate-800",
    },
    {
      key: "consumed",
      label: "Consumo actual",
      value: data.currentConsumed,
      labelClass: "text-red-500",
      valueClass: "text-red-600",
    },
    {
      key: "available",
      label: "Cupo neto disponible",
      value: data.availableLimit,
      labelClass: "text-emerald-600",
      valueClass: "text-emerald-600",
    },
  ];

  return (
    <div className="bg-white p-4 sm:p-6 rounded-2xl shadow-sm border border-slate-200/60 overflow-hidden flex flex-col relative w-full">
      <div className="absolute top-0 right-0 w-32 sm:w-48 h-32 sm:h-48 bg-slate-50 rounded-bl-full -z-10 opacity-60 pointer-events-none" />

      <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4 mb-5 sm:mb-6">
        <div className="flex items-start sm:items-center gap-3 min-w-0">
          <div className="bg-red-50 p-2.5 sm:p-3 rounded-xl shrink-0">
            <Icon path={mdiCreditCardSettings} size={1.2} className="text-red-600" />
          </div>
          <div className="min-w-0">
            <h3 className="text-sm sm:text-base font-bold text-slate-800 uppercase tracking-wider leading-snug">
              Detalle del cupo de credito
            </h3>
            <p className="text-xs sm:text-sm text-slate-400 mt-0.5">Cupos autorizados para anticipo de facturas</p>
          </div>
        </div>

        <button
          type="button"
          onClick={onRegisterRepayment}
          disabled={data.currentConsumed <= 0}
          className={`w-full sm:w-auto shrink-0 px-4 sm:px-5 py-3 sm:py-2.5 text-sm font-bold rounded-xl shadow-sm border flex items-center justify-center gap-2 transition-all ${data.currentConsumed > 0 ? "bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border-emerald-200 active:scale-95" : "bg-slate-50 text-slate-400 border-slate-200 cursor-not-allowed"}`}
        >
          <Icon path={mdiCashRegister} size={0.8} />
          Registrar abono
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 sm:gap-6 md:gap-8 pt-2 sm:pt-4 pb-2">
        {metrics.map((metric, index) => (
          <div
            key={metric.key}
            className={`min-w-0 ${
              index < metrics.length - 1
                ? "pb-4 border-b border-slate-100 sm:pb-0 sm:border-b-0 sm:border-r sm:pr-4 md:pr-6"
                : ""
            }`}
          >
            <p className={`text-[10px] sm:text-xs uppercase font-bold tracking-wider mb-1 ${metric.labelClass}`}>
              {metric.label}
            </p>
            <p className={`text-2xl sm:text-3xl font-black font-mono break-all leading-tight ${metric.valueClass}`}>
              $ {formatMoney(metric.value)}
            </p>
          </div>
        ))}
      </div>

      <div className="mt-6 sm:mt-8">
        <div className="flex flex-wrap justify-between gap-x-3 gap-y-1 text-xs sm:text-sm font-bold text-slate-500 mb-2">
          <span>Porcentaje de ocupación del cupo</span>
          <span className={isCritical ? "text-red-600" : "text-slate-700"}>{formatPercent(data.utilizationRate)}</span>
        </div>
        <div className="relative w-full bg-slate-100 h-2.5 sm:h-3 rounded-full overflow-hidden">
          <div
            className={`h-full transition-all duration-500 rounded-full ${isCritical ? "bg-gradient-to-r from-red-500 to-red-700" : "bg-gradient-to-r from-emerald-400 to-emerald-600"}`}
            style={{ width: `${Math.min(data.utilizationRate, 100)}%` }}
          />
          <div
            className="absolute top-0 h-full w-0.5 bg-slate-700/70"
            style={{ left: `${Math.min(data.warningThreshold, 100)}%` }}
            title={`Umbral de alerta: ${formatPercent(data.warningThreshold)}`}
          />
        </div>
        <p className="text-[10px] sm:text-xs text-slate-400 mt-1.5">
          Umbral de alerta: {formatPercent(data.warningThreshold)} de ocupación
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 mt-6 pt-5 border-t border-slate-100">
        {pricingItems.map((item) => (
          <div key={item.key} className="flex items-start gap-3 min-w-0 bg-slate-50 rounded-xl p-3 sm:p-4">
            <div className="bg-white p-2 rounded-lg text-red-600 shadow-sm shrink-0">
              <Icon path={item.icon} size={0.9} />
            </div>
            <div className="min-w-0">
              <p className="text-[10px] sm:text-xs uppercase font-bold tracking-wider text-slate-400">{item.label}</p>
              <p className="text-lg sm:text-xl font-black font-mono text-slate-800 leading-tight">{item.value}</p>
              {item.hint && <p className="text-[11px] text-slate-500 mt-0.5">{item.hint}</p>}
            </div>
          </div>
        ))}

        <div className={`flex items-start gap-3 min-w-0 rounded-xl p-3 sm:p-4 border ${thresholdStatus.container}`}>
          <div className={`bg-white p-2 rounded-lg shadow-sm shrink-0 ${thresholdStatus.iconClass}`}>
            <Icon path={thresholdStatus.icon} size={0.9} />
          </div>
          <div className="min-w-0">
            <p className="text-[10px] sm:text-xs uppercase font-bold tracking-wider text-slate-400">Estado del cupo</p>
            <span
              className={`inline-flex items-center gap-1.5 mt-1 px-2.5 py-1 rounded-full text-xs font-bold uppercase tracking-wider ${thresholdStatus.chip}`}
            >
              <span className={`w-1.5 h-1.5 rounded-full ${thresholdStatus.dot}`} />
              {thresholdStatus.label}
            </span>
            <p className="text-[11px] text-slate-500 mt-1">
              {isCritical
                ? `La ocupación alcanzó el umbral de ${formatPercent(data.warningThreshold)}`
                : `Por debajo del umbral de ${formatPercent(data.warningThreshold)}`}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CreditLineDetailCard;
