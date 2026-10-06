import React from "react";
import { HiOutlineBuildingOffice2 } from "react-icons/hi2";
import { formatMoney, formatPercent } from "../../utils/format.js";

const money = (value) => `$${formatMoney(value)}`;

const initial = (name) => (name ? name.trim().charAt(0).toUpperCase() : "P");

/** Barra de ocupación con la marca del umbral a partir del cual ya no se admiten cargas. */
const UtilizationBar = ({ utilization, threshold, dark }) => {
  const used = Math.min(Number(utilization) || 0, 100);
  const limit = Math.min(Number(threshold) || 0, 100);
  const critical = used >= limit;

  return (
    <div>
      <div
        className={`flex justify-between text-[10px] font-bold uppercase tracking-wider mb-1.5 ${
          dark ? "text-red-200" : "text-gray-500"
        }`}
      >
        <span>Ocupación</span>
        <span>{formatPercent(utilization)}</span>
      </div>
      <div className={`relative w-full h-2.5 rounded-full overflow-hidden ${dark ? "bg-red-950/80" : "bg-gray-200"}`}>
        <div
          className={`h-full rounded-full transition-all duration-700 ${critical ? "bg-red-500" : "bg-emerald-500"}`}
          style={{ width: `${used}%` }}
        />
        <div
          className={`absolute top-0 h-full w-0.5 ${dark ? "bg-white/80" : "bg-gray-700"}`}
          style={{ left: `${limit}%` }}
          title={`Umbral de carga: ${formatPercent(threshold)}`}
        />
      </div>
      <p className={`mt-1 text-[10px] ${dark ? "text-red-200" : "text-gray-400"}`}>
        Umbral de carga: {formatPercent(threshold)} del cupo
      </p>
    </div>
  );
};

const InactiveBadge = ({ status }) =>
  status && status !== "ACTIVE" ? (
    <span className="ml-2 inline-flex px-2 py-0.5 rounded-full text-[10px] font-bold bg-gray-200 text-gray-700">
      Línea inactiva
    </span>
  ) : null;

const Identity = ({ summary, dark }) => (
  <div className="flex items-center gap-3 min-w-0">
    <div
      className={`h-11 w-11 rounded-full flex items-center justify-center shrink-0 font-bold text-lg ${
        dark ? "bg-red-600 text-white" : "bg-red-50 text-red-700 border border-red-100"
      }`}
    >
      {initial(summary?.name)}
    </div>
    <div className="min-w-0">
      <p className="text-[10px] font-bold uppercase tracking-wider text-gray-500 flex items-center gap-1">
        <HiOutlineBuildingOffice2 className="w-3.5 h-3.5" /> Pagador
      </p>
      <p className="text-sm sm:text-base font-bold text-gray-900 break-words leading-tight">
        {summary?.name || "Sin nombre"}
      </p>
      <p className="text-xs text-gray-500 mt-0.5">
        NIT {summary?.nit || "-"} · Código {summary?.code || "-"}
      </p>
    </div>
  </div>
);

const LoadingCard = ({ className }) => (
  <div className={`bg-white rounded-lg shadow-sm border border-gray-200 p-4 animate-pulse ${className}`}>
    <div className="h-4 bg-gray-200 rounded w-1/2 mb-2" />
    <div className="h-3 bg-gray-100 rounded w-1/3" />
  </div>
);

/** Panel lateral rojo de la vista de carga: identidad arriba y estado de la línea debajo. */
const PanelVariant = ({ summary, creditLine }) => (
  <div className="flex-1 shadow-2xl bg-gradient-to-r from-red-600 to-red-800 text-white p-4 rounded-xl font-montserrat flex flex-col gap-4">
    <div className="bg-white rounded-lg shadow-lg p-4">
      <Identity summary={summary} />
    </div>

    {creditLine ? (
      <div className="bg-red-900/40 p-4 rounded-xl border border-red-500/30 shadow-inner">
        <h4 className="text-[10px] uppercase font-bold text-red-200 mb-3 tracking-wider flex items-center">
          Estado de la línea
          <InactiveBadge status={creditLine.status} />
        </h4>

        <div className="space-y-3">
          <div className="flex justify-between items-center border-b border-red-700/50 pb-2">
            <span className="text-xs text-red-100 font-medium">Cupo total</span>
            <span className="text-sm font-mono font-bold">{money(creditLine.limitAmount)}</span>
          </div>
          <div className="flex justify-between items-center border-b border-red-700/50 pb-2">
            <span className="text-xs text-red-100 font-medium">Consumo actual</span>
            <span className="text-sm font-mono font-bold">{money(creditLine.amountInUse)}</span>
          </div>
          <div className="flex justify-between items-center border-b border-red-700/50 pb-2">
            <span className="text-xs text-red-100 font-medium">Disponible</span>
            <span className="text-sm font-mono font-bold">{money(creditLine.availableAmount)}</span>
          </div>
          <div className="bg-red-950/50 -mx-2 px-2 py-2 rounded-lg">
            <div className="flex justify-between items-center">
              <span className="text-xs text-white font-bold">Disponible para cargar</span>
              <span className="text-sm font-mono font-bold text-emerald-400">
                {money(creditLine.availableToUpload)}
              </span>
            </div>
            <p className="text-[10px] text-red-200 mt-1">
              Tope de carga: {money(creditLine.uploadLimitAmount)} ({formatPercent(creditLine.thresholdPercentage)} del
              cupo)
            </p>
          </div>
        </div>

        <div className="mt-4 pt-3 border-t border-red-700/50">
          <UtilizationBar
            utilization={creditLine.utilizationPercentage}
            threshold={creditLine.thresholdPercentage}
            dark
          />
        </div>
      </div>
    ) : (
      <p className="text-xs text-red-100 bg-red-900/40 rounded-lg p-3 border border-red-500/30">
        El pagador no tiene una línea de crédito asignada.
      </p>
    )}
  </div>
);

const Metric = ({ label, value, highlight }) => (
  <div className={`rounded-lg px-3 py-2 ${highlight ? "bg-emerald-50 border border-emerald-100" : "bg-gray-50"}`}>
    <p className="text-[10px] font-bold uppercase tracking-wider text-gray-500">{label}</p>
    <p className={`text-sm font-mono font-bold ${highlight ? "text-emerald-700" : "text-gray-900"}`}>{value}</p>
  </div>
);

/** Franja horizontal de la bitácora: identidad a la izquierda y métricas de la línea a la derecha. */
const BannerVariant = ({ summary, creditLine }) => (
  <div className="bg-white border border-gray-200 border-l-4 border-l-red-600 rounded-lg shadow-sm p-4 font-montserrat flex flex-col xl:flex-row xl:items-center gap-4">
    <div className="xl:w-80 shrink-0">
      <Identity summary={summary} />
    </div>

    {creditLine ? (
      <div className="flex-1 grid grid-cols-2 md:grid-cols-5 gap-2 items-center">
        <Metric label="Cupo total" value={money(creditLine.limitAmount)} />
        <Metric label="Consumo actual" value={money(creditLine.amountInUse)} />
        <Metric label="Disponible" value={money(creditLine.availableAmount)} />
        <Metric label="Disponible para cargar" value={money(creditLine.availableToUpload)} highlight />
        <div className="col-span-2 md:col-span-1 px-1">
          <UtilizationBar utilization={creditLine.utilizationPercentage} threshold={creditLine.thresholdPercentage} />
        </div>
      </div>
    ) : (
      <p className="flex-1 text-xs text-gray-500">El pagador no tiene una línea de crédito asignada.</p>
    )}
  </div>
);

/**
 * Información del pagador autenticado.
 * @param variant "panel" (columna lateral de la carga) o "banner" (franja horizontal de la bitácora).
 */
const PayerSummaryCard = ({ summary, isLoading, variant = "banner" }) => {
  if (isLoading && !summary) return <LoadingCard className={variant === "panel" ? "" : "w-full"} />;
  if (!summary) return null;

  const creditLine = summary.creditLine ?? null;
  return variant === "panel" ? (
    <PanelVariant summary={summary} creditLine={creditLine} />
  ) : (
    <BannerVariant summary={summary} creditLine={creditLine} />
  );
};

export default PayerSummaryCard;
