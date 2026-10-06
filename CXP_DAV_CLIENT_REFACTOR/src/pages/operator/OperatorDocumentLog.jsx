import React, { useEffect, useState } from "react";
import { entityService } from "../../services/admin/entityService.js";
import usePayerSummary from "../../hooks/usePayerSummary.js";
import PayerDirectory from "../../components/payer/PayerDirectory.jsx";
import PayerSummaryCard from "../../components/payer/PayerSummaryCard.jsx";
import PayerDocumentHistory from "../../components/payer/PayerDocumentHistory.jsx";
import { showError } from "../../utils/errors.js";

/** Bitácora de documentos del operador bancario: consulta, en solo lectura, la de cualquier pagador. */
const OperatorDocumentLog = () => {
  const [payers, setPayers] = useState([]);
  const [isLoadingPayers, setIsLoadingPayers] = useState(true);
  const [selectedPayer, setSelectedPayer] = useState(null);
  const selectedPayerId = selectedPayer?.id ?? null;
  const { summary, isLoading: isLoadingSummary } = usePayerSummary(selectedPayerId);

  useEffect(() => {
    let cancelled = false;
    entityService
      .getPayers(0, 100)
      .then((page) => {
        if (cancelled) return;
        const content = page?.content ?? [];
        setPayers(content);
        setSelectedPayer(content[0] ?? null);
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("Error fetching payers:", error);
        showError("No se pudo cargar el directorio de pagadores");
      })
      .finally(() => {
        if (!cancelled) setIsLoadingPayers(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 gap-3 sm:gap-4">
      <div className="shrink-0 space-y-1">
        <h1 className="text-base sm:text-lg lg:text-xl font-bold text-gray-900 font-montserrat">
          Bitácora de documentos
        </h1>
        <p className="text-xs sm:text-sm text-gray-500">
          Seleccione un pagador para consultar el estado de sus documentos, agrupados por proveedor.
        </p>
      </div>

      <div className="flex flex-col lg:flex-row flex-1 min-h-0 gap-4">
        <aside className="lg:w-64 xl:w-72 shrink-0 flex flex-col min-h-0 shadow-2xl bg-gradient-to-r from-red-600 to-red-800 bg-red-800 text-white p-4 rounded-xl font-montserrat">
          <PayerDirectory
            payers={payers}
            isLoading={isLoadingPayers}
            selectedPayerId={selectedPayerId}
            onSelect={setSelectedPayer}
          />
        </aside>

        <div className="flex-1 min-w-0 min-h-0 flex flex-col gap-3 sm:gap-4">
          {selectedPayerId ? (
            <>
              <div className="shrink-0">
                <PayerSummaryCard summary={summary} isLoading={isLoadingSummary} variant="banner" />
              </div>
              <PayerDocumentHistory key={selectedPayerId} payerId={selectedPayerId} />
            </>
          ) : (
            !isLoadingPayers && (
              <div className="flex-1 flex items-center justify-center bg-white border border-gray-200 rounded-lg shadow-sm p-6 text-sm text-gray-500">
                Seleccione un pagador del directorio para ver su bitácora.
              </div>
            )
          )}
        </div>
      </div>
    </div>
  );
};

export default OperatorDocumentLog;
