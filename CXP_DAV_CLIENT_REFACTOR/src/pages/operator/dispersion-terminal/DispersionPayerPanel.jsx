import React from "react";
import Icon from "@mdi/react";
import { mdiFileDocumentMultiple, mdiDomain, mdiBankTransferOut } from "@mdi/js";

const DispersionPayerPanel = ({ payers, selectedPayerId, onSelectPayer, currentPayer, isLoading }) => {
  const pendingGroups = Number(currentPayer.pendingGroups || 0);
  const hasPayer = Boolean(selectedPayerId);

  return (
    <div className="lg:col-span-4 flex flex-col gap-3 sm:gap-4 lg:h-full lg:min-h-0 lg:overflow-hidden">
      <div className="bg-white p-4 sm:p-5 rounded-2xl shadow-sm border border-slate-200/60 shrink-0">
        <label className="flex items-center gap-2 text-xs font-bold text-slate-500 uppercase tracking-wider mb-3">
          <Icon path={mdiDomain} size={0.7} className="text-red-500" /> Seleccionar pagador
        </label>
        <select
          value={selectedPayerId}
          onChange={(e) => onSelectPayer(e.target.value)}
          disabled={payers.length === 0}
          className="w-full bg-slate-50 border border-slate-200 rounded-xl text-sm p-3 text-slate-700 font-medium focus:ring-2 focus:ring-red-100 focus:border-red-500 outline-none transition-all cursor-pointer disabled:opacity-50"
        >
          {payers.length === 0 ? (
            <option value="">{isLoading ? "Cargando pagadores..." : "Sin pagadores registrados"}</option>
          ) : (
            payers.map((payer) => (
              <option key={payer.id} value={payer.id}>
                {payer.name}
              </option>
            ))
          )}
        </select>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-slate-200/60 overflow-hidden flex-1 flex flex-col min-h-0">
        <div className="h-1.5 w-full bg-gradient-to-r from-red-500 to-red-700 shrink-0"></div>
        <div className="p-4 sm:p-6 flex-1 flex flex-col min-h-0 overflow-y-auto">
          <h2 className="text-base sm:text-lg font-bold text-slate-800 leading-tight mb-4 break-words">
            {currentPayer.name}
          </h2>

          <div className="space-y-4 mb-4 sm:mb-6">
            <div className="flex items-start gap-3">
              <div className="bg-red-50 p-2 rounded-lg mt-0.5 shrink-0">
                <Icon path={mdiBankTransferOut} size={0.8} className="text-red-600" />
              </div>
              <div className="min-w-0">
                <p className="text-[10px] uppercase tracking-wider font-bold text-slate-400">Cuenta de cargo</p>
                {currentPayer.accountNumber ? (
                  <p className="text-sm font-mono font-medium text-slate-700 break-words">
                    {currentPayer.accountNumber}
                  </p>
                ) : (
                  <p className="text-sm font-medium text-slate-500">Sin cuenta principal</p>
                )}
              </div>
            </div>
            {hasPayer && !currentPayer.accountNumber && (
              <div className="rounded-xl bg-amber-50 border border-amber-200 p-3 text-xs text-amber-800">
                El pagador no tiene una cuenta principal. Regístrela antes de generar lotes de dispersión.
              </div>
            )}
          </div>

          <div className="bg-slate-50 border border-slate-100 rounded-xl p-4 flex items-center justify-between gap-3">
            <p className="text-[10px] uppercase tracking-wider font-bold text-slate-500">Lotes por dispersar</p>
            <div className="flex items-center gap-2 shrink-0">
              <span className="text-2xl sm:text-3xl font-black text-red-600">{pendingGroups}</span>
              <Icon path={mdiFileDocumentMultiple} size={1} className="text-red-200" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DispersionPayerPanel;
