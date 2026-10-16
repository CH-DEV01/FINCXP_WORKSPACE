import React from "react";

import SearchInput from "./SearchInput";
import useServerPagination from "../hooks/useServerPagination";

/** Tamaño de página sugerido para quien arma `fetchPage`. */
export const DEFAULT_PAGE_SIZE = 6;

const defaultIdentifiers = (entity) => (entity?.nit ? `NIT ${entity.nit}` : "");

const logLoadError = (error) => console.error("Error al buscar entidades", error);

/**
 * Selector con búsqueda paginada en el servidor (nombre / NIT).
 * `fetchPage({ search, page })` debe devolver una Page de Spring y memoizarse con useCallback.
 * Con `selected` muestra la entidad elegida; `locked` oculta el botón Cambiar.
 */
const EntitySearchPicker = ({
  fetchPage,
  selected,
  onSelect,
  hasError = false,
  locked = false,
  disabled = false,
  placeholder = "Buscar por nombre o NIT",
  emptyLabel = "No hay entidades que coincidan.",
  loadingLabel = "Cargando...",
  identifiers = defaultIdentifiers,
  className = "",
}) => {
  const canSearch = Boolean(fetchPage) && !selected && !disabled && !locked;

  const { items, totalPages, isLoading, searchTerm, setSearchTerm, currentPage, setCurrentPage } = useServerPagination(
    canSearch ? fetchPage : null,
    { onError: logLoadError },
  );

  const borderClass = hasError ? "border-red-500" : "border-gray-300";

  if (selected) {
    return (
      <div
        className={`flex items-center justify-between gap-3 px-3 py-2 border rounded-lg ${borderClass} ${
          locked || disabled ? "bg-gray-100 text-gray-500" : ""
        } ${className}`}
      >
        <div className="min-w-0">
          <p className="text-xs font-medium text-gray-800 truncate">{selected.name}</p>
          {identifiers(selected) && <p className="text-[10px] text-gray-500">{identifiers(selected)}</p>}
        </div>
        {!locked && !disabled && (
          <button
            type="button"
            onClick={() => onSelect(null)}
            className="shrink-0 text-xs text-red-600 hover:text-red-700 hover:underline cursor-pointer"
          >
            Cambiar
          </button>
        )}
      </div>
    );
  }

  if (disabled || locked) {
    return (
      <p className={`w-full px-3 py-2 border rounded-lg text-xs text-gray-400 bg-gray-50 ${borderClass} ${className}`}>
        No hay entidad seleccionada
      </p>
    );
  }

  return (
    <div className={`border rounded-lg p-2 space-y-2 ${borderClass} ${className}`}>
      <SearchInput
        value={searchTerm}
        onChange={setSearchTerm}
        placeholder={placeholder}
        ariaLabel={placeholder}
      />

      <ul className="max-h-48 overflow-y-auto divide-y divide-gray-100">
        {isLoading && <li className="px-2 py-2 text-xs text-gray-400">{loadingLabel}</li>}
        {!isLoading && items.length === 0 && <li className="px-2 py-2 text-xs text-gray-400">{emptyLabel}</li>}
        {!isLoading &&
          items.map((entity) => (
            <li key={entity.id}>
              <button
                type="button"
                onClick={() => onSelect(entity)}
                className="w-full text-left px-2 py-2 rounded-md hover:bg-red-50 cursor-pointer"
              >
                <span className="block text-xs text-gray-800">{entity.name}</span>
                {identifiers(entity) && (
                  <span className="block text-[10px] text-gray-500">{identifiers(entity)}</span>
                )}
              </button>
            </li>
          ))}
      </ul>

      {totalPages > 1 && (
        <div className="flex items-center justify-between text-[10px] text-gray-500">
          <button
            type="button"
            disabled={currentPage <= 1 || isLoading}
            onClick={() => setCurrentPage(currentPage - 1)}
            className="px-2 py-1 rounded hover:bg-gray-100 disabled:opacity-40 cursor-pointer disabled:cursor-default"
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
            className="px-2 py-1 rounded hover:bg-gray-100 disabled:opacity-40 cursor-pointer disabled:cursor-default"
          >
            Siguiente
          </button>
        </div>
      )}
    </div>
  );
};

export default EntitySearchPicker;
