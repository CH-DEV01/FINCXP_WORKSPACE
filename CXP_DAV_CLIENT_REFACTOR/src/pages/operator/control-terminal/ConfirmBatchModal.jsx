import React from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiCheckAll, mdiClose } from "@mdi/js";
import { formatMoney, formatDate } from "../../../utils/format";

const ConfirmBatchModal = ({ isOpen, onClose, onConfirm, batchData, isConfirming }) => {
  if (!isOpen) return null;

  const docs = batchData?.documents || [];
  // Redondeo por proveedor, igual que la carta, para que el total cuadre con ella.
  const amountBySupplier = docs.reduce((acc, doc) => {
    acc[doc.supplierName] = (acc[doc.supplierName] || 0) + Number(doc.amountToFinance || 0);
    return acc;
  }, {});
  const totalAmount = Object.values(amountBySupplier).reduce((sum, amount) => sum + Math.round(amount * 100) / 100, 0);
  const canConfirm = docs.length > 0;

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-0 sm:p-4">
      <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-xl w-full max-w-5xl flex flex-col h-[92dvh] sm:h-auto sm:max-h-[90vh] overflow-hidden">
        <div className="p-4 sm:p-6 border-b border-slate-100 flex justify-between items-start gap-3 shrink-0">
          <div className="min-w-0">
            <h2 className="text-base sm:text-xl font-bold text-slate-800 mb-1 break-words">
              Confirmar desembolso del Lote: {batchData?.batchNumber}
            </h2>
            <p className="text-xs sm:text-sm text-slate-500">
              Al confirmar, <span className="font-semibold text-slate-700">todos</span> los documentos del lote se
              marcarán como desembolsados. La confirmación aplica al lote completo.
            </p>
            <div className="flex flex-wrap gap-x-4 gap-y-1 mt-2 text-xs text-slate-600">
              {batchData?.payerName && (
                <span>
                  Pagador: <span className="font-semibold text-slate-800">{batchData.payerName}</span>
                </span>
              )}
              <span>
                Desembolso:{" "}
                <span className="font-semibold text-slate-800">{formatDate(batchData?.disbursementDate)}</span>
              </span>
              <span>
                Vencimiento: <span className="font-semibold text-slate-800">{formatDate(batchData?.dueDate)}</span>
              </span>
              <span>
                Solicitud: <span className="font-semibold text-slate-800">{formatDate(batchData?.requestDate)}</span>
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            disabled={isConfirming}
            className="p-2 text-slate-400 hover:text-red-600 rounded-lg shrink-0"
          >
            <Icon path={mdiClose} size={1} />
          </button>
        </div>

        <div className="overflow-y-auto p-3 sm:p-6 flex-1 min-h-0 bg-slate-50/30">
          {/* Móvil: cards */}
          <div className="md:hidden space-y-3">
            {docs.map((doc) => (
              <div key={doc.id} className="rounded-xl border border-slate-200 bg-white p-3 space-y-2.5">
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-slate-500">Documento</span>
                  <span className="font-medium text-slate-800 truncate">{doc.documentNumber}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-slate-500">Proveedor</span>
                  <span className="font-medium text-slate-700 text-right break-words">{doc.supplierName}</span>
                </div>
                <div className="flex justify-between items-center gap-2 text-sm">
                  <span className="text-slate-500">Fecha programada</span>
                  <span className="font-medium text-slate-700">{formatDate(doc.scheduledDisbursementDate)}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm border-t border-slate-100 pt-2">
                  <span className="text-slate-500">Monto a desembolsar</span>
                  <span className="font-mono font-semibold text-slate-800">${formatMoney(doc.amountToFinance)}</span>
                </div>
              </div>
            ))}
          </div>

          {/* Desktop: tabla */}
          <div className="hidden md:block overflow-x-auto">
            <table className="w-full text-left text-sm bg-white border border-slate-200 rounded-lg overflow-hidden">
              <thead className="bg-slate-100 text-xs font-bold text-slate-600 uppercase tracking-wider">
                <tr>
                  <th className="p-4">No. Documento</th>
                  <th className="p-4">Proveedor</th>
                  <th className="p-4">Fecha programada</th>
                  <th className="p-4 text-right">Monto a desembolsar</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {docs.map((doc) => (
                  <tr key={doc.id} className="hover:bg-slate-50">
                    <td className="p-4 font-medium text-slate-700">{doc.documentNumber}</td>
                    <td className="p-4 text-slate-600">{doc.supplierName}</td>
                    <td className="p-4 text-slate-600">{formatDate(doc.scheduledDisbursementDate)}</td>
                    <td className="p-4 text-right font-mono font-medium text-slate-800">
                      ${formatMoney(doc.amountToFinance)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="p-4 sm:p-6 flex flex-col sm:flex-row sm:items-center gap-3 bg-white shrink-0 border-t border-slate-100">
          <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs font-semibold text-slate-600 sm:mr-auto">
            <span>{docs.length} documento(s)</span>
            <span className="font-mono text-slate-800">Total a desembolsar: ${formatMoney(totalAmount)}</span>
          </div>
          <div className="flex flex-col-reverse sm:flex-row gap-3">
            <button
              onClick={onClose}
              disabled={isConfirming}
              className="w-full sm:w-auto px-5 py-2.5 text-sm font-bold text-slate-600 hover:bg-slate-100 rounded-xl transition-all disabled:opacity-50"
            >
              Cancelar
            </button>
            <button
              onClick={onConfirm}
              disabled={isConfirming || !canConfirm}
              className="w-full sm:w-auto px-6 py-2.5 text-sm font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl shadow-md flex items-center justify-center transition-all disabled:opacity-50"
            >
              {isConfirming ? (
                <>
                  <Icon path={mdiLoading} size={0.8} className="animate-spin mr-2" /> Procesando...
                </>
              ) : (
                <>
                  <Icon path={mdiCheckAll} size={0.9} className="mr-2" /> Confirmar lote completo
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConfirmBatchModal;
