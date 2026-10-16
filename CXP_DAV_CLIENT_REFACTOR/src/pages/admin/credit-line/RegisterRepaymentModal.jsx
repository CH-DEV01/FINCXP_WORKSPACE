import React, { useState, useEffect } from "react";
import Icon from "@mdi/react";
import { mdiCheckCircleOutline, mdiLoading, mdiClose, mdiFormatListChecks, mdiCurrencyUsd } from "@mdi/js";
import Swal from "sweetalert2";
import { creditFacilityService } from "../../../services/admin/creditFacilityService";
import { formatDate, formatMoney } from "../../../utils/format";
import { apiErrorMessage } from "../../../utils/errors";

const batchLabel = (batch) =>
  `${batch.batchNumber} · vence ${formatDate(batch.dueDate)} · $ ${formatMoney(batch.nominalAmount)}`;

const RegisterRepaymentModal = ({ isOpen, onClose, onConfirm, isProcessing, currentConsumed, creditFacilityId }) => {
  const [repaymentType, setRepaymentType] = useState("PARTIAL");
  const [referenceNumber, setReferenceNumber] = useState("");
  const [batches, setBatches] = useState([]);
  const [isLoadingBatches, setIsLoadingBatches] = useState(false);
  const [batchesError, setBatchesError] = useState("");
  const [selectedBatchId, setSelectedBatchId] = useState("");

  useEffect(() => {
    if (!isOpen) return;
    setRepaymentType("PARTIAL");
    setReferenceNumber("");
    setSelectedBatchId("");
    setBatches([]);
    setBatchesError("");
    if (!creditFacilityId) return;

    let cancelled = false;
    setIsLoadingBatches(true);
    creditFacilityService
      .getRestorableBatches(creditFacilityId)
      .then((result) => {
        if (!cancelled) setBatches(result);
      })
      .catch((error) => {
        if (!cancelled) setBatchesError(apiErrorMessage(error, "No se pudieron cargar los lotes de desembolso."));
      })
      .finally(() => {
        if (!cancelled) setIsLoadingBatches(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isOpen, creditFacilityId]);

  if (!isOpen) return null;

  const isPartial = repaymentType === "PARTIAL";
  const selectedBatch = batches.find((batch) => batch.id === selectedBatchId);
  // El servidor ignora amount: PARTIAL usa el nominal del lote; FULL, el consumo exacto
  // (scale 4). No redondear a 2 decimales ni comparar contra el cupo (VAL-03/VAL-04).
  const displayAmount = isPartial
    ? (selectedBatch?.nominalAmount != null ? String(selectedBatch.nominalAmount) : "")
    : String(currentConsumed ?? "");
  const canSubmit = !isProcessing && (!isPartial || Boolean(selectedBatch));

  const handleSubmit = (e) => {
    e.preventDefault();

    if (isPartial && !selectedBatch) {
      Swal.fire("Atención", "Seleccione el lote de desembolso que liquida el pago parcial.", "warning");
      return;
    }

    if (!isPartial && !(Number(currentConsumed) > 0)) {
      Swal.fire("Atención", "No hay consumo que restaurar.", "warning");
      return;
    }

    const reference = referenceNumber.trim();
    if (!reference) {
      Swal.fire("Atención", "Debe especificar el número de comprobante o referencia de pago.", "warning");
      return;
    }
    if (reference.length > 255) {
      Swal.fire("Atención", "La referencia no puede superar 255 caracteres.", "warning");
      return;
    }

    onConfirm({
      // FULL: sin monto; el API liquida el consumo actual. PARTIAL: opcional (el API usa el lote).
      amount: isPartial ? displayAmount : undefined,
      reference,
      type: repaymentType,
      disbursementBatchId: isPartial ? selectedBatch.id : undefined,
      batchNumber: isPartial ? selectedBatch.batchNumber : undefined,
    });
  };

  const renderBatchSelector = () => {
    if (isLoadingBatches) {
      return (
        <div className="flex items-center gap-2 text-xs text-slate-500 p-3 bg-slate-50 rounded-xl border border-slate-200">
          <Icon path={mdiLoading} size={0.6} className="animate-spin" />
          Cargando lotes desembolsados...
        </div>
      );
    }
    if (batchesError) {
      return <p className="text-xs text-red-600 p-3 bg-red-50 rounded-xl border border-red-100">{batchesError}</p>;
    }
    if (batches.length === 0) {
      return (
        <p className="text-xs text-slate-500 p-3 bg-slate-50 rounded-xl border border-slate-200">
          No hay lotes desembolsados pendientes de abono para este pagador.
        </p>
      );
    }
    return (
      <>
        <select
          value={selectedBatchId}
          onChange={(e) => setSelectedBatchId(e.target.value)}
          disabled={isProcessing}
          className="w-full bg-slate-50 border border-slate-200 rounded-xl text-sm p-3 text-slate-700 outline-none focus:ring-2 focus:ring-red-100 focus:border-red-500 transition-all"
        >
          <option value="">Seleccione un lote...</option>
          {batches.map((batch) => (
            <option key={batch.id} value={batch.id}>
              {batchLabel(batch)}
            </option>
          ))}
        </select>
        {selectedBatch && (
          <dl className="mt-2 grid grid-cols-2 gap-x-3 gap-y-1 text-[11px] text-slate-500">
            <dt>Documentos</dt>
            <dd className="text-right font-semibold text-slate-700">{selectedBatch.documentCount}</dd>
            <dt>Fecha de desembolso</dt>
            <dd className="text-right font-semibold text-slate-700">{formatDate(selectedBatch.disbursementDate)}</dd>
            <dt>Monto desembolsado</dt>
            <dd className="text-right font-mono font-semibold text-slate-700">
              $ {formatMoney(selectedBatch.disbursedAmount)}
            </dd>
          </dl>
        )}
      </>
    );
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

          {isPartial && (
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Lote de desembolso</label>
              {renderBatchSelector()}
            </div>
          )}

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">Monto del Abono ($)</label>
            <div className="relative">
              <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                <span className="text-slate-500 font-bold">$</span>
              </div>
              <input
                type="text"
                readOnly
                placeholder={isPartial ? "Seleccione un lote" : "0.00"}
                value={displayAmount === "" ? "" : formatMoney(displayAmount)}
                className="w-full border rounded-xl text-sm py-3 pr-4 pl-8 font-mono outline-none bg-slate-100 border-slate-200 text-slate-500 cursor-not-allowed font-bold"
              />
            </div>
            {isPartial ? (
              <p className="text-[11px] text-slate-400 mt-1">
                Se restaura el valor nominal de los documentos del lote, que es lo que consumieron del cupo.
              </p>
            ) : (
              <p className="text-[11px] text-slate-400 mt-1">
                Liquida todo el consumo actual y marca como abonados los lotes desembolsados pendientes.
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-1">
              Referencia / Comprobante de Pago
            </label>
            <input
              type="text"
              required
              maxLength={255}
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
              disabled={!canSubmit}
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
