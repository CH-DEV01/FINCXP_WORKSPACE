import React from "react";
import Icon from "@mdi/react";
import { mdiDomain } from "@mdi/js";

const CreditLineHeader = ({ payers, activePayer, onSelectPayer, isLoadingPayers }) => (
  <div className="flex flex-col lg:flex-row justify-between items-stretch lg:items-center mb-4 sm:mb-6 gap-4 bg-white p-4 sm:p-5 rounded-2xl shadow-sm border border-slate-200/60">
    <div className="flex items-center gap-3 min-w-0">
      <div className="bg-red-50 p-2.5 rounded-xl text-red-600 hidden sm:block shrink-0">
        <Icon path={mdiDomain} size={1.2} />
      </div>
      <div className="min-w-0">
        <h2 className="text-lg sm:text-xl font-bold text-slate-800 leading-tight">Mesa de cupos de crédito</h2>
        <p className="text-xs sm:text-sm text-slate-500 mt-0.5">Monitoreo y redención de cupos de crédito</p>
      </div>
    </div>

    <div className="w-full lg:w-96 lg:max-w-full relative shrink-0">
      <label className="block text-[10px] uppercase font-bold text-slate-400 mb-1 ml-1">Seleccionar cliente</label>
      <select
        value={activePayer}
        onChange={(e) => onSelectPayer(e.target.value)}
        disabled={isLoadingPayers || payers.length === 0}
        className="w-full appearance-none bg-slate-50 border border-slate-200 text-slate-700 py-3 pl-4 pr-10 rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-red-100 focus:border-red-500 text-sm font-bold cursor-pointer transition-all disabled:opacity-50"
      >
        {isLoadingPayers ? (
          <option value="">Cargando clientes...</option>
        ) : payers.length === 0 ? (
          <option value="">No hay clientes registrados</option>
        ) : (
          payers.map((payer) => (
            <option key={payer.id} value={payer.id}>
              {payer.name}
            </option>
          ))
        )}
      </select>
      <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 pt-5 text-slate-400">
        <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
        </svg>
      </div>
    </div>
  </div>
);

export default CreditLineHeader;
