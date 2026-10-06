import React from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiInboxOutline, mdiClose } from "@mdi/js";
import RequestStatusBadge from "./RequestStatusBadge";
import { formatMoney, formatDate } from "../../../utils/format";

const RequestDetailModal = ({ request, payerName, suppliers, isLoading, onClose }) => {
  if (!request) return null;

  const totalDocuments = suppliers.reduce((sum, s) => sum + Number(s.documentCount || 0), 0);
  const totalAmount = suppliers.reduce((sum, s) => sum + Number(s.totalAmount || 0), 0);
  const totalAmountToCredit = suppliers.reduce((sum, s) => sum + Number(s.amountToCredit || 0), 0);

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-0 sm:p-4">
      <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-xl w-full max-w-4xl flex flex-col h-[85dvh] sm:h-auto sm:max-h-[90vh] overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100 flex justify-between items-start gap-3 shrink-0">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2 mb-1">
              <h2 className="text-base sm:text-xl font-bold text-slate-800 break-words">
                Detalle de la solicitud
                {request.batchNumber ? `: ${request.batchNumber}` : ""}
              </h2>
              <RequestStatusBadge status={request.status} />
            </div>
            <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2 text-xs text-slate-600">
              {payerName && (
                <span>
                  Pagador: <span className="font-semibold text-slate-800">{payerName}</span>
                </span>
              )}
              <span>
                Desembolso: <span className="font-semibold text-slate-800">{formatDate(request.disbursementDate)}</span>
              </span>
              <span>
                Vencimiento: <span className="font-semibold text-slate-800">{formatDate(request.dueDate)}</span>
              </span>
              <span>
                Solicitado por el cliente:{" "}
                <span className="font-semibold text-slate-800">{formatDate(request.requestDate)}</span>
              </span>
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-red-600 rounded-lg shrink-0">
            <Icon path={mdiClose} size={1} />
          </button>
        </div>

        <div className="overflow-y-auto p-3 sm:p-6 flex-1 min-h-0 bg-slate-50/30">
          {isLoading ? (
            <div className="py-16 flex flex-col items-center justify-center text-slate-400 gap-2">
              <Icon path={mdiLoading} size={1.2} className="animate-spin" />
              <p className="text-sm font-medium text-slate-600">Cargando detalle...</p>
            </div>
          ) : suppliers.length === 0 ? (
            <div className="py-16 flex flex-col items-center justify-center text-slate-400 gap-2">
              <Icon path={mdiInboxOutline} size={1.6} />
              <p className="text-sm font-medium text-slate-600">La solicitud no tiene documentos</p>
            </div>
          ) : (
            <>
              {/* Móvil: cards */}
              <div className="md:hidden space-y-3">
                {suppliers.map((supplier) => (
                  <div key={supplier.supplierId} className="rounded-xl border border-slate-200 bg-white p-3 space-y-2">
                    <p className="text-sm font-bold text-slate-800 break-words">{supplier.supplierName}</p>
                    <div className="flex justify-between gap-2 text-sm">
                      <span className="text-slate-500">Registros</span>
                      <span className="font-bold bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md">
                        {supplier.documentCount}
                      </span>
                    </div>
                    <div className="flex justify-between gap-2 text-sm">
                      <span className="text-slate-500">Cuenta a abonar</span>
                      <span className="font-mono text-slate-700">
                        {supplier.accountNumber || "Sin cuenta principal"}
                      </span>
                    </div>
                    <div className="flex justify-between gap-2 text-sm">
                      <span className="text-slate-500">Vencimiento</span>
                      <span className="font-medium text-slate-700">{formatDate(supplier.dueDate)}</span>
                    </div>
                    <div className="flex justify-between gap-2 text-sm border-t border-slate-100 pt-2">
                      <span className="text-slate-500">Monto a desembolsar</span>
                      <span className="font-mono font-semibold text-slate-800">
                        ${formatMoney(supplier.totalAmount)}
                      </span>
                    </div>
                    <div className="flex justify-between gap-2 text-sm">
                      <span className="text-slate-500">Monto a abonar</span>
                      <span className="font-mono font-semibold text-slate-800">
                        ${formatMoney(supplier.amountToCredit)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>

              {/* Desktop: tabla */}
              <div className="hidden md:block overflow-x-auto">
                <table className="w-full text-left text-sm bg-white border border-slate-200 rounded-lg overflow-hidden">
                  <thead className="bg-slate-100 text-xs font-bold text-slate-600 uppercase tracking-wider">
                    <tr>
                      <th className="p-4">Proveedor</th>
                      <th className="p-4 text-center">Registros</th>
                      <th className="p-4">Cuenta a abonar</th>
                      <th className="p-4 text-right">Monto a desembolsar</th>
                      <th className="p-4 text-right">Monto a abonar</th>
                      <th className="p-4">Vencimiento</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {suppliers.map((supplier) => (
                      <tr key={supplier.supplierId} className="hover:bg-slate-50">
                        <td className="p-4 font-medium text-slate-700">{supplier.supplierName}</td>
                        <td className="p-4 text-center">
                          <span className="text-xs font-bold bg-slate-100 text-slate-600 px-2.5 py-1 rounded-md">
                            {supplier.documentCount}
                          </span>
                        </td>
                        <td className="p-4 font-mono text-slate-600">
                          {supplier.accountNumber || (
                            <span className="font-sans text-xs text-red-600">Sin cuenta principal</span>
                          )}
                        </td>
                        <td className="p-4 text-right font-mono font-medium text-slate-800">
                          ${formatMoney(supplier.totalAmount)}
                        </td>
                        <td className="p-4 text-right font-mono font-medium text-slate-800">
                          ${formatMoney(supplier.amountToCredit)}
                        </td>
                        <td className="p-4 text-slate-600">{formatDate(supplier.dueDate)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>

        <div className="p-4 sm:p-6 flex flex-col sm:flex-row sm:items-center gap-3 bg-white shrink-0 border-t border-slate-100">
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs font-semibold text-slate-600 sm:mr-auto">
            <span>{suppliers.length} proveedor(es)</span>
            <span>{totalDocuments} registro(s)</span>
            <span className="font-mono text-slate-800">A desembolsar: ${formatMoney(totalAmount)}</span>
            <span className="font-mono text-slate-800">A abonar: ${formatMoney(totalAmountToCredit)}</span>
          </div>
          <button
            onClick={onClose}
            className="w-full sm:w-auto px-5 py-2.5 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-all"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};

export default RequestDetailModal;
