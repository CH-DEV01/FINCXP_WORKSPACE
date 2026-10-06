import React, { useState } from "react";
import LoadingSpinner from "../LoadingSpinner.jsx";

const matches = (payer, term) =>
  !term ||
  payer.name?.toLowerCase().includes(term) ||
  payer.nit?.includes(term) ||
  payer.code?.toLowerCase().includes(term);

/** Directorio de pagadores con búsqueda por nombre, NIT o código. Pensado para un contenedor rojo. */
const PayerDirectory = ({ payers, isLoading, selectedPayerId, onSelect }) => {
  const [searchTerm, setSearchTerm] = useState("");
  const term = searchTerm.trim().toLowerCase();
  const filteredPayers = payers.filter((payer) => matches(payer, term));

  return (
    <div className="flex flex-col flex-1 min-h-0">
      <div className="mb-2 flex-shrink-0">
        <h3 className="text-xs font-bold text-red-200 mb-2 uppercase tracking-wider">Directorio de Pagadores</h3>
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg className="h-4 w-4 text-red-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </div>
          <input
            type="text"
            className="block w-full pl-10 pr-3 py-2 border border-red-500/50 rounded-lg leading-5 bg-red-900/40 text-white placeholder-red-300 focus:outline-none focus:bg-white focus:text-gray-900 focus:placeholder-gray-400 sm:text-sm transition-colors"
            placeholder="Buscar por nombre, NIT o código..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      <div className="border-b-2 border-red-700/50 mb-3 flex-shrink-0"></div>

      <div className="flex-1 relative min-h-[300px]">
        <div className="absolute inset-0 overflow-y-auto pr-2 custom-scrollbar">
          {isLoading ? (
            <div className="flex justify-center py-4">
              <LoadingSpinner />
            </div>
          ) : payers.length === 0 ? (
            <div className="text-center py-6 text-red-200 text-sm bg-red-900/30 rounded-lg border border-red-800">
              No hay pagadores registrados.
            </div>
          ) : filteredPayers.length === 0 ? (
            <div className="text-center py-6 text-red-200 text-sm bg-red-900/30 rounded-lg border border-red-800">
              No se encontraron pagadores que coincidan con &quot;{searchTerm}&quot;.
            </div>
          ) : (
            <div className="w-full space-y-2">
              {filteredPayers.map((payer) => {
                const active = selectedPayerId === payer.id;
                return (
                  <button
                    type="button"
                    key={payer.id}
                    onClick={() => onSelect(payer)}
                    className={`w-full text-left p-3 rounded-lg transition-all duration-200 cursor-pointer flex items-center space-x-3 ${
                      active
                        ? "bg-white text-gray-900 shadow-md transform scale-[1.02]"
                        : "bg-red-800/50 hover:bg-red-700/80 border border-red-700/50 text-white"
                    }`}
                  >
                    <div
                      className={`h-8 w-8 rounded-full flex items-center justify-center shrink-0 ${
                        active ? "bg-red-600 text-white" : "bg-red-900 text-red-200"
                      }`}
                    >
                      <span className="font-bold text-sm">{payer.name ? payer.name.charAt(0).toUpperCase() : "P"}</span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <h3 className={`font-bold text-sm truncate ${active ? "text-gray-900" : "text-white"}`}>
                        {payer.name || "Sin Nombre"}
                      </h3>
                      <p className={`text-xs truncate mt-0.5 ${active ? "text-red-600 font-medium" : "text-red-200"}`}>
                        NIT: {payer.nit || "-"}
                      </p>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default PayerDirectory;
