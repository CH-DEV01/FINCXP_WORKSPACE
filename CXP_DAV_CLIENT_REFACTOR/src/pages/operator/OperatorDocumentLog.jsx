import React, { useEffect, useState } from "react";
import { FaAngleDoubleLeft } from "react-icons/fa";
import { entityService } from "../../services/admin/entityService.js";
import usePayerSummary from "../../hooks/usePayerSummary.js";
import PayerDirectory from "../../components/payer/PayerDirectory.jsx";
import PayerSummaryCard from "../../components/payer/PayerSummaryCard.jsx";
import PayerDocumentHistory from "../../components/payer/PayerDocumentHistory.jsx";
import { showError } from "../../utils/errors.js";

const DIRECTORY_HIDDEN_KEY = "operatorDocumentLog.directoryHidden";

/** Bitácora de documentos del operador bancario: consulta, en solo lectura, la de cualquier pagador. */
const OperatorDocumentLog = () => {
  const [isDirectoryHidden, setIsDirectoryHidden] = useState(
    () => localStorage.getItem(DIRECTORY_HIDDEN_KEY) === "true",
  );
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

  const toggleDirectory = () => {
    setIsDirectoryHidden((hidden) => {
      localStorage.setItem(DIRECTORY_HIDDEN_KEY, String(!hidden));
      return !hidden;
    });
  };

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
        <aside
          className={`relative shrink-0 flex flex-col min-h-0 overflow-hidden shadow-2xl bg-gradient-to-r from-red-600 to-red-800 bg-red-800 text-white rounded-xl font-montserrat transition-all duration-300 ease-in-out ${
            isDirectoryHidden ? "max-h-14 lg:max-h-none lg:w-12" : "max-h-[80vh] lg:max-h-none lg:w-64 xl:w-72"
          }`}
        >
          <button
            type="button"
            onClick={toggleDirectory}
            aria-expanded={!isDirectoryHidden}
            aria-controls="payer-directory"
            title={isDirectoryHidden ? "Mostrar directorio de pagadores" : "Ocultar directorio de pagadores"}
            aria-label={isDirectoryHidden ? "Mostrar directorio de pagadores" : "Ocultar directorio de pagadores"}
            className="absolute top-3 right-2.5 z-10 p-1.5 rounded-md text-red-100 hover:text-white hover:bg-red-900/50 transition-colors cursor-pointer"
          >
            <FaAngleDoubleLeft
              className={`transition-transform duration-300 ${
                isDirectoryHidden ? "-rotate-90 lg:rotate-180" : "rotate-90 lg:rotate-0"
              }`}
            />
          </button>

          <span
            aria-hidden="true"
            className={`absolute left-4 top-4 lg:left-1/2 lg:top-14 lg:-translate-x-1/2 lg:[writing-mode:vertical-rl] lg:rotate-180 text-xs font-bold uppercase tracking-wider text-red-100 whitespace-nowrap transition-opacity duration-200 ${
              isDirectoryHidden ? "opacity-100 delay-150" : "opacity-0 pointer-events-none"
            }`}
          >
            Directorio de pagadores
          </span>

          <div
            id="payer-directory"
            inert={isDirectoryHidden}
            className={`flex flex-col flex-1 min-h-0 p-4 lg:w-64 xl:w-72 transition-opacity duration-200 ${
              isDirectoryHidden ? "opacity-0" : "opacity-100 delay-150"
            }`}
          >
            <PayerDirectory
              payers={payers}
              isLoading={isLoadingPayers}
              selectedPayerId={selectedPayerId}
              onSelect={setSelectedPayer}
            />
          </div>
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
