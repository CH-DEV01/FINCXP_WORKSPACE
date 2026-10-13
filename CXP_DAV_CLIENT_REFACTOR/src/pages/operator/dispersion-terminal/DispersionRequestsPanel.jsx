import React, { useMemo, useState } from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiFormatListBulleted, mdiInboxOutline, mdiDownloadMultiple } from "@mdi/js";
import { DISPERSION_REQUEST_STATUS } from "../../../constants/status";
import DispersionStatusBadge from "./DispersionStatusBadge";
import DispersionRequestActions from "./DispersionRequestActions";
import { dispersionRequestKey } from "./dispersionUtils";
import { formatMoney, formatDate } from "../../../utils/format";

const EmptyState = ({ text, icon, spin = false }) => (
  <div className="flex flex-col items-center justify-center text-slate-400 gap-2">
    <Icon path={icon} size={spin ? 1.2 : 1.6} className={spin ? "animate-spin" : ""} />
    <p className="text-sm font-medium text-slate-600">{text}</p>
  </div>
);

const LOADING_STATE = <EmptyState text="Cargando solicitudes..." icon={mdiLoading} spin />;
const NO_REQUESTS_STATE = (
  <EmptyState text="No hay documentos por dispersar para este pagador" icon={mdiInboxOutline} />
);
const NO_MATCHES_STATE = <EmptyState text="No hay solicitudes con el estado seleccionado" icon={mdiInboxOutline} />;

const pendingDate = <span className="text-slate-300">—</span>;

const ALL = "ALL";
const STATUS_OPTIONS = [
  { id: ALL, label: "Todos" },
  ...Object.entries(DISPERSION_REQUEST_STATUS).map(([id, { label }]) => ({ id, label })),
];

const CHECKBOX_CLASS = "h-4 w-4 accent-red-600 cursor-pointer disabled:cursor-not-allowed disabled:opacity-50";

const isSelectable = (request) => request.status === "INGRESADO";

const DispersionRequestsPanel = ({
  requests: allRequests,
  isLoading,
  isGeneratingAll,
  onGenerateSelected,
  selectedDueDates,
  onToggleDueDate,
  onToggleAllDueDates,
  onClearSelection,
  ...actionProps
}) => {
  const [statusFilter, setStatusFilter] = useState(ALL);

  const countByStatus = useMemo(
    () => allRequests.reduce((acc, r) => ({ ...acc, [r.status]: (acc[r.status] || 0) + 1 }), {}),
    [allRequests],
  );
  const requests = useMemo(
    () => (statusFilter === ALL ? allRequests : allRequests.filter((r) => r.status === statusFilter)),
    [allRequests, statusFilter],
  );

  const visibleSelectable = requests.filter(isSelectable).map((r) => r.dueDate);
  const selectedVisible = visibleSelectable.filter((dueDate) => selectedDueDates.has(dueDate)).length;
  const allVisibleSelected = visibleSelectable.length > 0 && selectedVisible === visibleSelectable.length;
  const selectedCount = allRequests.filter((r) => isSelectable(r) && selectedDueDates.has(r.dueDate)).length;
  const selectionDisabled = isGeneratingAll || isLoading;

  const rowCheckbox = (request) =>
    isSelectable(request) ? (
      <input
        type="checkbox"
        className={CHECKBOX_CLASS}
        checked={selectedDueDates.has(request.dueDate)}
        onChange={() => onToggleDueDate(request.dueDate)}
        disabled={selectionDisabled}
        aria-label={`Seleccionar la solicitud con vencimiento ${formatDate(request.dueDate)}`}
      />
    ) : null;

  const showLoading = isLoading && requests.length === 0;
  const emptyState = allRequests.length === 0 ? NO_REQUESTS_STATE : NO_MATCHES_STATE;
  const actions = (request) => <DispersionRequestActions request={request} {...actionProps} />;

  return (
    <div className="lg:col-span-8 min-h-[420px] lg:min-h-0 lg:h-full">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/60 overflow-hidden h-full flex flex-col min-h-0">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-wrap items-center gap-2 sm:gap-3 bg-slate-50/50 shrink-0">
          <Icon path={mdiFormatListBulleted} size={0.9} className="text-slate-400" />
          <h2 className="text-xs sm:text-sm font-bold text-slate-700 uppercase tracking-wider">
            Solicitudes de dispersión
          </h2>

          <div className="w-full sm:w-auto sm:ml-auto flex flex-col sm:flex-row sm:items-center gap-2">
            <label htmlFor="dispersion-status-filter" className="sr-only">
              Filtrar por estado
            </label>
            <select
              id="dispersion-status-filter"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full sm:w-44 px-3 py-2 border border-gray-200 rounded-md bg-white shadow-sm text-xs font-montserrat text-gray-700 focus:outline-none focus:ring-2 focus:ring-red-300 cursor-pointer"
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.label} ({option.id === ALL ? allRequests.length : countByStatus[option.id] || 0})
                </option>
              ))}
            </select>

            {selectedCount > 0 && (
              <button
                type="button"
                onClick={onClearSelection}
                disabled={selectionDisabled}
                className="px-3 py-2 rounded-md text-xs font-semibold text-slate-600 border border-slate-200 bg-white hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Limpiar
              </button>
            )}

            <button
              type="button"
              onClick={onGenerateSelected}
              disabled={selectedCount === 0 || selectionDisabled}
              title={
                selectedCount === 0
                  ? "Seleccione las solicitudes en estado Generado que desea descargar"
                  : "Genera los lotes seleccionados, los pasa a En proceso y descarga sus cartas"
              }
              className="inline-flex items-center justify-center gap-2 px-3 py-2 rounded-md text-xs font-bold text-white bg-red-600 hover:bg-red-700 shadow-sm transition-colors disabled:bg-slate-300 disabled:cursor-not-allowed"
            >
              <Icon path={isGeneratingAll ? mdiLoading : mdiDownloadMultiple} size={0.7} spin={isGeneratingAll} />
              {isGeneratingAll
                ? "Generando..."
                : `Descargar seleccionados${selectedCount > 0 ? ` (${selectedCount})` : ""}`}
            </button>
          </div>
        </div>

        <div className="flex-1 min-h-0 overflow-auto">
          {/* Móvil: cards */}
          <div className="md:hidden p-3 space-y-3">
            {showLoading ? (
              <div className="py-16">{LOADING_STATE}</div>
            ) : requests.length === 0 ? (
              <div className="py-16">{emptyState}</div>
            ) : (
              requests.map((request) => (
                <div key={dispersionRequestKey(request)} className="rounded-xl border border-slate-200 p-3 space-y-2">
                  <div className="flex justify-between items-center gap-2 text-sm">
                    <span className="flex items-center gap-2 min-w-0">
                      {rowCheckbox(request)}
                      <span className="font-bold text-slate-700 truncate">{request.batchNumber || "Sin lote"}</span>
                    </span>
                    <DispersionStatusBadge status={request.status} />
                  </div>
                  {[
                    ["Vencimiento", formatDate(request.dueDate)],
                    ["Dispersión", request.dispersionDate ? formatDate(request.dispersionDate) : pendingDate],
                    ["Proveedores", request.supplierCount],
                    ["Documentos", request.documentCount],
                  ].map(([label, value]) => (
                    <div key={label} className="flex justify-between gap-2 text-sm">
                      <span className="text-slate-500">{label}</span>
                      <span className="font-medium text-slate-700">{value}</span>
                    </div>
                  ))}
                  <div className="flex justify-between gap-2 text-sm border-t border-slate-100 pt-2">
                    <span className="text-slate-500">Monto a dispersar</span>
                    <span className="font-bold text-slate-800">${formatMoney(request.totalAmount)}</span>
                  </div>
                  <div className="flex justify-end pt-1">{actions(request)}</div>
                </div>
              ))
            )}
          </div>

          {/* Desktop: tabla */}
          <div className="hidden md:block min-w-full">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead className="sticky top-0 z-10 bg-white">
                <tr className="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <th className="pl-4 lg:pl-5 pr-0 py-3 lg:py-4 w-8">
                    <input
                      type="checkbox"
                      className={CHECKBOX_CLASS}
                      checked={allVisibleSelected}
                      ref={(input) => {
                        if (input) input.indeterminate = selectedVisible > 0 && !allVisibleSelected;
                      }}
                      onChange={() => onToggleAllDueDates(visibleSelectable, !allVisibleSelected)}
                      disabled={selectionDisabled || visibleSelectable.length === 0}
                      title="Seleccionar todas las solicitudes en estado Generado"
                      aria-label="Seleccionar todas las solicitudes en estado Generado"
                    />
                  </th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4"># de lote</th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4">Vencimiento</th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4">Dispersión</th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4 text-center">Proveedores</th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4 text-center">Documentos</th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4 text-right">Monto a dispersar</th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4 text-center">Estado</th>
                  <th className="px-4 lg:px-5 py-3 lg:py-4 text-center">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {showLoading ? (
                  <tr>
                    <td colSpan="9" className="px-5 py-16 text-center">
                      {LOADING_STATE}
                    </td>
                  </tr>
                ) : requests.length === 0 ? (
                  <tr>
                    <td colSpan="9" className="px-5 py-16 text-center">
                      {emptyState}
                    </td>
                  </tr>
                ) : (
                  requests.map((request) => (
                    <tr
                      key={dispersionRequestKey(request)}
                      className={`hover:bg-slate-50 transition-colors ${
                        isSelectable(request) && selectedDueDates.has(request.dueDate) ? "bg-red-50/40" : ""
                      }`}
                    >
                      <td className="pl-4 lg:pl-5 pr-0 py-3 lg:py-4">{rowCheckbox(request)}</td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-xs font-bold text-slate-700">
                        {request.batchNumber || pendingDate}
                      </td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-xs font-semibold text-slate-700">
                        {formatDate(request.dueDate)}
                      </td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-xs font-medium text-slate-600">
                        {request.dispersionDate ? formatDate(request.dispersionDate) : pendingDate}
                      </td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-center">
                        <span className="text-xs font-bold bg-slate-100 text-slate-600 px-2.5 py-1 rounded-md">
                          {request.supplierCount}
                        </span>
                      </td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-center">
                        <span className="text-xs font-bold bg-slate-100 text-slate-600 px-2.5 py-1 rounded-md">
                          {request.documentCount}
                        </span>
                      </td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-right">
                        <span className="text-sm font-bold text-slate-800">${formatMoney(request.totalAmount)}</span>
                      </td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-center">
                        <DispersionStatusBadge status={request.status} />
                      </td>
                      <td className="px-4 lg:px-5 py-3 lg:py-4 text-center">{actions(request)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DispersionRequestsPanel;
