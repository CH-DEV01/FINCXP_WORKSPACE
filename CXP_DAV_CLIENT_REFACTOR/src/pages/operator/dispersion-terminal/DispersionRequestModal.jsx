import React from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiInboxOutline, mdiClose } from "@mdi/js";
import DispersionStatusBadge from "./DispersionStatusBadge";
import { formatMoney, formatDate } from "../../../utils/format";

const HeaderItem = ({ label, children }) => (
  <span>
    {label}: <span className="font-semibold text-slate-800">{children}</span>
  </span>
);

const AccountCell = ({ accountNumber }) =>
  accountNumber || <span className="font-sans text-xs text-red-600">Sin cuenta principal</span>;

/** Una fila por proveedor y vencimiento, como en la carta de solicitud de dispersión. */
const groupBySupplier = (documents) => {
  const groups = new Map();
  documents.forEach((doc) => {
    const key = `${doc.supplierId}|${doc.dueDate}`;
    const group = groups.get(key);
    if (group) {
      group.recordCount += 1;
      group.totalAmount += Number(doc.nominalAmount || 0);
    } else {
      groups.set(key, {
        key,
        supplierName: doc.supplierName,
        accountNumber: doc.accountNumber,
        dueDate: doc.dueDate,
        recordCount: 1,
        totalAmount: Number(doc.nominalAmount || 0),
      });
    }
  });
  return [...groups.values()];
};

/**
 * Detalle de una solicitud de dispersión. Si está pendiente permite generar el lote, que se dispersa
 * en la misma fecha de vencimiento; si el lote está en proceso, confirmar la dispersión.
 */
const DispersionRequestModal = ({
  request,
  payerName,
  payerAccountNumber,
  documents,
  signerName,
  isLoading,
  isSubmitting,
  onGenerate,
  onConfirm,
  onClose,
}) => {
  if (!request) return null;

  const isPending = request.status === "INGRESADO";
  const isInProcess = request.status === "EN_PROCESO";
  const totalAmount = documents.reduce((sum, d) => sum + Number(d.nominalAmount || 0), 0);
  const rows = groupBySupplier(documents);
  const supplierCount = new Set(documents.map((d) => d.supplierId)).size;
  const loadedCount = documents.filter((d) => d.status === "APPROVED").length;
  const canGenerate = isPending && !isLoading && documents.length > 0 && Boolean(payerAccountNumber);

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-0 sm:p-4">
      <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-xl w-full max-w-5xl flex flex-col h-[85dvh] sm:h-auto sm:max-h-[90vh] overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100 flex justify-between items-start gap-3 shrink-0">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2 mb-1">
              <h2 className="text-base sm:text-xl font-bold text-slate-800 break-words">
                Solicitud de dispersión{request.batchNumber ? `: ${request.batchNumber}` : ""}
              </h2>
              <DispersionStatusBadge status={request.status} />
            </div>
            <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2 text-xs text-slate-600">
              {payerName && <HeaderItem label="Pagador">{payerName}</HeaderItem>}
              <HeaderItem label="Cuenta de cargo">{payerAccountNumber || "Sin cuenta principal"}</HeaderItem>
              <HeaderItem label="Vencimiento y dispersión">{formatDate(request.dueDate)}</HeaderItem>
              {signerName && <HeaderItem label="Firma">{signerName}</HeaderItem>}
            </div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-red-600 rounded-lg shrink-0">
            <Icon path={mdiClose} size={1} />
          </button>
        </div>

        <div className="overflow-y-auto p-3 sm:p-6 flex-1 min-h-0 bg-slate-50/30 space-y-4">
          {isPending && loadedCount > 0 && !isLoading && (
            <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 text-xs text-amber-800">
              {loadedCount} documento(s) siguen cargados pero el proveedor ya no puede solicitarlos. Al generar el lote
              pasarán a no financiables y su monto se liberará del cupo antes de dispersarse.
            </div>
          )}

          {isLoading ? (
            <div className="py-16 flex flex-col items-center justify-center text-slate-400 gap-2">
              <Icon path={mdiLoading} size={1.2} className="animate-spin" />
              <p className="text-sm font-medium text-slate-600">Cargando detalle...</p>
            </div>
          ) : documents.length === 0 ? (
            <div className="py-16 flex flex-col items-center justify-center text-slate-400 gap-2">
              <Icon path={mdiInboxOutline} size={1.6} />
              <p className="text-sm font-medium text-slate-600">La solicitud no tiene documentos</p>
            </div>
          ) : (
            <>
              {/* Móvil: cards */}
              <div className="md:hidden space-y-3">
                {rows.map((row) => (
                  <div key={row.key} className="rounded-xl border border-slate-200 bg-white p-3 space-y-2 text-sm">
                    <p className="font-bold text-slate-800 break-words">{row.supplierName}</p>
                    <div className="flex justify-between gap-2">
                      <span className="text-slate-500">Cuenta a abonar</span>
                      <span className="font-mono text-slate-700">
                        <AccountCell accountNumber={row.accountNumber} />
                      </span>
                    </div>
                    <div className="flex justify-between gap-2">
                      <span className="text-slate-500">Registros</span>
                      <span className="font-semibold text-slate-700">{row.recordCount}</span>
                    </div>
                    <div className="flex justify-between gap-2">
                      <span className="text-slate-500">Fecha de vencimiento</span>
                      <span className="text-slate-700">{formatDate(row.dueDate)}</span>
                    </div>
                    <div className="flex justify-between gap-2 border-t border-slate-100 pt-2">
                      <span className="text-slate-500">Monto total</span>
                      <span className="font-mono font-semibold text-slate-800">${formatMoney(row.totalAmount)}</span>
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
                      <th className="p-4">Cuenta a abonar</th>
                      <th className="p-4 text-center">Registros</th>
                      <th className="p-4">Fecha de vencimiento</th>
                      <th className="p-4 text-right">Monto total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {rows.map((row) => (
                      <tr key={row.key} className="hover:bg-slate-50">
                        <td className="p-4 font-medium text-slate-700">{row.supplierName}</td>
                        <td className="p-4 font-mono text-slate-600">
                          <AccountCell accountNumber={row.accountNumber} />
                        </td>
                        <td className="p-4 text-center font-semibold text-slate-700">{row.recordCount}</td>
                        <td className="p-4 text-slate-600 whitespace-nowrap">{formatDate(row.dueDate)}</td>
                        <td className="p-4 text-right font-mono font-medium text-slate-800">
                          ${formatMoney(row.totalAmount)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>

        <div className="p-4 sm:p-6 flex flex-col lg:flex-row lg:items-center gap-3 bg-white shrink-0 border-t border-slate-100">
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs font-semibold text-slate-600 lg:mr-auto">
            <span>{supplierCount} proveedor(es)</span>
            <span>{documents.length} documento(s)</span>
            <span className="font-mono text-slate-800">A dispersar: ${formatMoney(totalAmount)}</span>
          </div>

          <div className="flex flex-col-reverse sm:flex-row gap-2">
            <button
              onClick={onClose}
              disabled={isSubmitting}
              className="w-full sm:w-auto px-5 py-2.5 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-all disabled:opacity-40"
            >
              Cerrar
            </button>
            {isPending && (
              <button
                onClick={onGenerate}
                disabled={!canGenerate || isSubmitting}
                className="w-full sm:w-auto px-5 py-2.5 text-sm font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl transition-all disabled:opacity-40 flex items-center justify-center gap-2"
              >
                {isSubmitting && <Icon path={mdiLoading} size={0.7} className="animate-spin" />}
                Generar lote y descargar solicitud
              </button>
            )}
            {isInProcess && (
              <button
                onClick={onConfirm}
                disabled={isLoading || isSubmitting}
                className="w-full sm:w-auto px-5 py-2.5 text-sm font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl transition-all disabled:opacity-40 flex items-center justify-center gap-2"
              >
                {isSubmitting && <Icon path={mdiLoading} size={0.7} className="animate-spin" />}
                Confirmar dispersión
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DispersionRequestModal;
