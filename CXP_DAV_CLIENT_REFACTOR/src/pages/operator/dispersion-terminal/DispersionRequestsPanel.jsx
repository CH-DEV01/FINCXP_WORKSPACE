import React from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiFormatListBulleted, mdiInboxOutline } from "@mdi/js";
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

const pendingDate = <span className="text-slate-300">—</span>;

const DispersionRequestsPanel = ({ requests, isLoading, ...actionProps }) => {
  const showLoading = isLoading && requests.length === 0;
  const actions = (request) => <DispersionRequestActions request={request} {...actionProps} />;

  return (
    <div className="lg:col-span-8 min-h-[420px] lg:min-h-0 lg:h-full">
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/60 overflow-hidden h-full flex flex-col min-h-0">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center gap-2 bg-slate-50/50 shrink-0">
          <Icon path={mdiFormatListBulleted} size={0.9} className="text-slate-400" />
          <h2 className="text-xs sm:text-sm font-bold text-slate-700 uppercase tracking-wider">
            Solicitudes de dispersión
          </h2>
        </div>

        <div className="flex-1 min-h-0 overflow-auto">
          {/* Móvil: cards */}
          <div className="md:hidden p-3 space-y-3">
            {showLoading ? (
              <div className="py-16">{LOADING_STATE}</div>
            ) : requests.length === 0 ? (
              <div className="py-16">{NO_REQUESTS_STATE}</div>
            ) : (
              requests.map((request) => (
                <div key={dispersionRequestKey(request)} className="rounded-xl border border-slate-200 p-3 space-y-2">
                  <div className="flex justify-between items-center gap-2 text-sm">
                    <span className="font-bold text-slate-700 truncate">{request.batchNumber || "Sin lote"}</span>
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
                    <td colSpan="8" className="px-5 py-16 text-center">
                      {LOADING_STATE}
                    </td>
                  </tr>
                ) : requests.length === 0 ? (
                  <tr>
                    <td colSpan="8" className="px-5 py-16 text-center">
                      {NO_REQUESTS_STATE}
                    </td>
                  </tr>
                ) : (
                  requests.map((request) => (
                    <tr key={dispersionRequestKey(request)} className="hover:bg-slate-50 transition-colors">
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
