import React, { useState, useEffect } from "react";
import Icon from "@mdi/react";
import { mdiCheckCircleOutline, mdiLoading, mdiClose, mdiFormatListChecks, mdiCurrencyUsd } from "@mdi/js";
import Swal from "sweetalert2";
import { formatAmountMask, formatMoney } from "../../../utils/format";

const RegisterRepaymentModal = ({ isOpen, onClose, onConfirm, isProcessing, currentConsumed }) => {
  const [repaymentType, setRepaymentType] = useState("PARTIAL");
  const [repaymentAmount, setRepaymentAmount] = useState("");
  const [referenceNumber, setReferenceNumber] = useState("");

  useEffect(() => {
    if (isOpen) {
      setRepaymentType("PARTIAL");
      setRepaymentAmount("");
      setReferenceNumber("");
    }
  }, [isOpen]);

  useEffect(() => {
    if (repaymentType === "FULL") {
      const exactAmount = Number(currentConsumed).toFixed(2);
      setRepaymentAmount(exactAmount);
    } else {
      setRepaymentAmount("");
    }
  }, [repaymentType, currentConsumed]);

  if (!isOpen) return null;

  const handleAmountChange = (e) => {
    const rawValue = e.target.value.replace(/,/g, "");
    if (/^\d*\.?\d{0,2}$/.test(rawValue)) {
      setRepaymentAmount(rawValue);
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const amount = Number(repaymentAmount);

    if (isNaN(amount) || amount <= 0) {
      Swal.fire("Atención", "Por favor ingresa un monto válido mayor a 0.", "warning");
      return;
    }

    if (amount > currentConsumed) {
      Swal.fire("Monto excedido", "El abono no puede ser mayor al consumo actual de la línea.", "warning");
      return;
    }

    if (!referenceNumber.trim()) {
      Swal.fire("Atención", "Debe especificar el número de comprobante o referencia de pago.", "warning");
      return;
    }

    onConfirm({ amount, reference: referenceNumber, type: repaymentType });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 backdrop-blur-sm p-0 sm:p-4">
      <div className="bg-white rounded-t-2xl sm:rounded-2xl shadow-xl w-full max-w-md max-h-[92dvh] sm:max-h-[90vh] flex flex-col overflow-hidden animate-fade-in">
        <div className="flex justify-between items-start gap-3 p-4 sm:p-5 border-b border-slate-100 bg-slate-50/50 shrink-0">
          <div className="min-w-0">
            <h3 className="text-base sm:text-md font-bold text-slate-800">Registrar abono a cupo</h3>
            <p className="text-xs text-slate-500 mt-0.5">Libera cupo disponible</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isProcessing}
            aria-label="Cerrar"
            className="p-2 -mr-1 text-slate-400 hover:text-red-600 rounded-lg transition-colors shrink-0"
          >
            <Icon path={mdiClose} size={0.9} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-4 sm:p-5 space-y-4 overflow-y-auto overscroll-contain">
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Consumo actual a redimir</label>
            <div className="bg-red-50 text-red-800 font-mono font-bold text-xs sm:text-sm p-3 rounded-xl border border-red-100 flex flex-col sm:flex-row sm:justify-between sm:items-center gap-1">
              <span className="font-sans font-semibold text-red-700/80">Monto consumido</span>
              <span className="break-all">$ {formatMoney(currentConsumed)}</span>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Tipo de redención</label>
            <div className="grid grid-cols-1 min-[400px]:grid-cols-2 gap-2 sm:gap-3">
              <label
                className={`flex items-center justify-center gap-2 p-3 rounded-xl border cursor-pointer transition-all ${repaymentType === "PARTIAL" ? "bg-slate-800 border-slate-800 text-white" : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"}`}
              >
                <input
                  type="radio"
                  name="repaymentType"
                  value="PARTIAL"
                  checked={repaymentType === "PARTIAL"}
                  onChange={() => setRepaymentType("PARTIAL")}
                  className="hidden"
                />
                <Icon path={mdiCurrencyUsd} size={0.7} />
                <span className="text-xs font-bold">Pago parcial</span>
              </label>
              <label
                className={`flex items-center justify-center gap-2 p-3 rounded-xl border cursor-pointer transition-all ${repaymentType === "FULL" ? "bg-slate-800 border-slate-800 text-white" : "bg-white border-slate-200 text-slate-600 hover:bg-slate-50"}`}
              >
                <input
                  type="radio"
                  name="repaymentType"
                  value="FULL"
                  checked={repaymentType === "FULL"}
                  onChange={() => setRepaymentType("FULL")}
                  className="hidden"
                />
                <Icon path={mdiFormatListChecks} size={0.7} />
                <span className="text-xs font-bold">Liquidación total</span>
              </label>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Monto del Abono ($)</label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <span className="text-slate-500 font-bold">$</span>
              </div>
              <input
                type="text"
                inputMode="decimal"
                required
                placeholder="0.00"
                value={formatAmountMask(repaymentAmount)}
                onChange={handleAmountChange}
                disabled={repaymentType === "FULL" || isProcessing}
                className={`w-full border rounded-xl text-sm py-3 pr-4 pl-8 font-mono text-slate-800 outline-none focus:ring-2 focus:ring-red-100 focus:border-red-500 transition-all ${repaymentType === "FULL" ? "bg-slate-100 border-slate-200 text-slate-500 cursor-not-allowed font-bold" : "bg-slate-50 border-slate-200 shadow-inner"}`}
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
              Referencia / Comprobante de Pago
            </label>
            <input
              type="text"
              required
              placeholder="Ej: DEP-9827364-DAV"
              value={referenceNumber}
              onChange={(e) => setReferenceNumber(e.target.value)}
              disabled={isProcessing}
              className="w-full bg-slate-50 border border-slate-200 rounded-xl text-sm p-3 text-slate-700 outline-none focus:ring-2 focus:ring-red-100 focus:border-red-500 transition-all"
            />
          </div>

          <div className="pt-3 border-t border-slate-100 flex flex-col-reverse sm:flex-row sm:justify-end gap-2 sm:gap-3 sticky bottom-0 bg-white pb-[env(safe-area-inset-bottom)]">
            <button
              type="button"
              onClick={onClose}
              disabled={isProcessing}
              className="w-full sm:w-auto px-4 py-3 sm:py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-600 text-xs font-bold rounded-xl transition-all"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isProcessing}
              className="w-full sm:w-auto px-5 py-3 sm:py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl shadow-md transition-all flex items-center justify-center gap-1.5 disabled:opacity-70 disabled:cursor-not-allowed"
            >
              {isProcessing ? (
                <>
                  <Icon path={mdiLoading} size={0.6} className="animate-spin" />
                  Aplicando abono...
                </>
              ) : (
                <>
                  <Icon path={mdiCheckCircleOutline} size={0.7} />
                  Aplicar y restaurar cupo
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default RegisterRepaymentModal;
