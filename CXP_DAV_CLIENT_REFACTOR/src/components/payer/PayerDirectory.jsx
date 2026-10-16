import React, { useCallback, useEffect, useRef } from "react";

import { entityService } from "../../services/admin/entityService.js";
import useServerPagination from "../../hooks/useServerPagination.js";

const PAGE_SIZE = 10;

const logLoadError = (error) => console.error("Error al cargar el directorio de pagadores", error);

/**
 * Directorio de pagadores con búsqueda paginada en el servidor (nombre / NIT).
 * Pensado para un contenedor rojo. Si `autoSelectFirst`, elige el primer resultado
 * de la página 1 sin filtro cuando aún no hay selección.
 */
const PayerDirectory = ({ selectedPayerId, onSelect, autoSelectFirst = true }) => {
  const fetchPayers = useCallback(
    ({ search, page }) => entityService.getPayerSummaries({ search, page, size: PAGE_SIZE }),
    [],
  );

  const { items, totalPages, isLoading, searchTerm, setSearchTerm, currentPage, setCurrentPage } = useServerPagination(
    fetchPayers,
    { onError: logLoadError },
  );

  const onSelectRef = useRef(onSelect);
  useEffect(() => {
    onSelectRef.current = onSelect;
  });

  useEffect(() => {
    if (!autoSelectFirst || selectedPayerId || isLoading || items.length === 0) return;
    if (searchTerm.trim() || currentPage !== 1) return;
    onSelectRef.current(items[0]);
  }, [autoSelectFirst, selectedPayerId, isLoading, items, searchTerm, currentPage]);

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
            placeholder="Buscar por nombre o NIT..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            aria-label="Buscar pagador por nombre o NIT"
          />
        </div>
      </div>

      <div className="border-b-2 border-red-700/50 mb-3 flex-shrink-0"></div>

      <div className="flex-1 relative min-h-[300px]">
        <div className="absolute inset-0 overflow-y-auto pr-2 custom-scrollbar">
          {isLoading ? (
            <div className="flex justify-center py-8" role="status" aria-label="Cargando pagadores">
              <div className="h-8 w-8 rounded-full border-2 border-red-200 border-t-white animate-spin" />
            </div>
          ) : items.length === 0 ? (
            <div className="text-center py-6 text-red-200 text-sm bg-red-900/30 rounded-lg border border-red-800">
              {searchTerm.trim()
                ? `No se encontraron pagadores que coincidan con "${searchTerm}".`
                : "No hay pagadores registrados."}
            </div>
          ) : (
            <div className="w-full space-y-2">
              {items.map((payer) => {
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

      {totalPages > 1 && (
        <div className="mt-3 flex items-center justify-between gap-2 text-[10px] text-red-100 flex-shrink-0">
          <button
            type="button"
            disabled={currentPage <= 1 || isLoading}
            onClick={() => setCurrentPage(currentPage - 1)}
            className="px-2 py-1 rounded bg-red-900/40 hover:bg-red-900/70 disabled:opacity-40 cursor-pointer disabled:cursor-default"
          >
            Anterior
          </button>
          <span>
            Página {currentPage} de {totalPages}
          </span>
          <button
            type="button"
            disabled={currentPage >= totalPages || isLoading}
            onClick={() => setCurrentPage(currentPage + 1)}
            className="px-2 py-1 rounded bg-red-900/40 hover:bg-red-900/70 disabled:opacity-40 cursor-pointer disabled:cursor-default"
          >
            Siguiente
          </button>
        </div>
      )}
    </div>
  );
};

export default PayerDirectory;
