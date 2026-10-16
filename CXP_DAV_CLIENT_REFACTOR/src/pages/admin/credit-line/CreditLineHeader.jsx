import React, { useCallback } from "react";
import Icon from "@mdi/react";
import { mdiDomain } from "@mdi/js";

import EntitySearchPicker, { DEFAULT_PAGE_SIZE } from "../../../components/EntitySearchPicker";
import { entityService } from "../../../services/admin/entityService";

const CreditLineHeader = ({ selectedPayer, onSelectPayer, isLoadingPayers }) => {
  const fetchPayers = useCallback(
    ({ search, page }) => entityService.getPayerSummaries({ search, page, size: DEFAULT_PAGE_SIZE }),
    [],
  );

  return (
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

      <div className="w-full lg:w-96 lg:max-w-full shrink-0">
        <label className="block text-[10px] uppercase font-bold text-slate-400 mb-1 ml-1">Seleccionar cliente</label>
        {isLoadingPayers && !selectedPayer ? (
          <p className="w-full px-3 py-3 border border-slate-200 rounded-xl text-sm text-slate-400 bg-slate-50">
            Cargando clientes...
          </p>
        ) : (
          <EntitySearchPicker
            fetchPage={fetchPayers}
            selected={selectedPayer}
            onSelect={onSelectPayer}
            placeholder="Buscar pagador por nombre o NIT"
            emptyLabel="No hay pagadores que coincidan."
            loadingLabel="Cargando pagadores..."
          />
        )}
      </div>
    </div>
  );
};

export default CreditLineHeader;
