import React, { useCallback } from "react";

import EntitySearchPicker, { DEFAULT_PAGE_SIZE } from "../../../components/EntitySearchPicker";
import { entityService } from "../../../services/admin/entityService";

/**
 * Selector de la entidad del usuario: solo lista las entidades del tipo que exige el rol y
 * busca en el servidor por nombre o NIT. Montarlo con key={roleId} para reiniciar la búsqueda.
 */
const EntityPicker = ({ roleId, selected, onSelect, hasError }) => {
  const fetchEntities = useCallback(
    ({ search, page }) => entityService.getEntitiesForRole(roleId, { search, page, size: DEFAULT_PAGE_SIZE }),
    [roleId],
  );

  if (!roleId) {
    return (
      <p
        className={`w-full px-3 py-2 border rounded-lg text-xs text-gray-400 bg-gray-50 ${
          hasError ? "border-red-500" : "border-gray-300"
        }`}
      >
        Seleccione primero un rol
      </p>
    );
  }

  return (
    <EntitySearchPicker
      fetchPage={fetchEntities}
      selected={selected}
      onSelect={onSelect}
      hasError={hasError}
      emptyLabel="No hay entidades que coincidan."
      loadingLabel="Cargando entidades..."
    />
  );
};

export default EntityPicker;
