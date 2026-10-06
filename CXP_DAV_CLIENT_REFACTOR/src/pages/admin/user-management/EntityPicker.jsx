import React, { useCallback } from "react";

import SearchInput from "../../../components/SearchInput";
import { entityService } from "../../../services/admin/entityService";
import useServerPagination from "../../../hooks/useServerPagination";

const RESULTS_PER_PAGE = 6;

const logLoadError = (error) => console.error("Error al cargar las entidades del rol", error);

const identifiers = (entity) => (entity.nit ? `NIT ${entity.nit}` : "");

/**
 * Selector de la entidad del usuario: solo lista las entidades del tipo que exige el rol y
 * busca en el servidor por nombre o NIT. Montarlo con key={roleId} para reiniciar la búsqueda.
 */
const EntityPicker = ({ roleId, selected, onSelect, hasError }) => {
  const fetchEntities = useCallback(
    ({ search, page }) => entityService.getEntitiesForRole(roleId, { search, page, size: RESULTS_PER_PAGE }),
    [roleId],
  );

  const { items, totalPages, isLoading, searchTerm, setSearchTerm, currentPage, setCurrentPage } = useServerPagination(
    roleId && !selected ? fetchEntities : null,
    { onError: logLoadError },
  );

  const borderClass = hasError ? "border-red-500" : "border-gray-300";

  if (!roleId) {
    return (
      <p className={`w-full px-3 py-2 border rounded-lg text-xs text-gray-400 bg-gray-50 ${borderClass}`}>
        Seleccione primero un rol
      </p>
    );
  }

  if (selected) {
    return (
      <div className={`flex items-center justify-between gap-3 px-3 py-2 border rounded-lg ${borderClass}`}>
        <div className="min-w-0">
          <p className="text-xs font-medium text-gray-800 truncate">{selected.name}</p>
          {identifiers(selected) && <p className="text-[10px] text-gray-500">{identifiers(selected)}</p>}
        </div>
        <button
          type="button"
          onClick={() => onSelect(null)}
          className="shrink-0 text-xs text-red-600 hover:text-red-700 hover:underline cursor-pointer"
        >
          Cambiar
        </button>
      </div>
    );
  }

  return (
    <div className={`border rounded-lg p-2 space-y-2 ${borderClass}`}>
      <SearchInput
        value={searchTerm}
        onChange={setSearchTerm}
        placeholder="Buscar por nombre o NIT"
        ariaLabel="Buscar entidad"
      />

      <ul className="max-h-48 overflow-y-auto divide-y divide-gray-100">
        {isLoading && <li className="px-2 py-2 text-xs text-gray-400">Cargando entidades...</li>}
        {!isLoading && items.length === 0 && (
          <li className="px-2 py-2 text-xs text-gray-400">No hay entidades que coincidan.</li>
        )}
        {!isLoading &&
          items.map((entity) => (
            <li key={entity.id}>
              <button
                type="button"
                onClick={() => onSelect(entity)}
                className="w-full text-left px-2 py-2 rounded-md hover:bg-red-50 cursor-pointer"
              >
                <span className="block text-xs text-gray-800">{entity.name}</span>
                {identifiers(entity) && <span className="block text-[10px] text-gray-500">{identifiers(entity)}</span>}
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

export default EntityPicker;
