import React from "react";
import { useAuth } from "../../context/AuthContext.jsx";
import usePayerSummary from "../../hooks/usePayerSummary.js";
import PayerSummaryCard from "../../components/payer/PayerSummaryCard.jsx";
import PayerDocumentHistory from "../../components/payer/PayerDocumentHistory.jsx";

const PayerDocumentLog = () => {
  const { user } = useAuth();
  const { summary, isLoading, reload } = usePayerSummary();

  return (
    <div className="flex flex-col w-full lg:h-[calc(100dvh-5.5rem)] min-h-0 gap-3 sm:gap-4">
      <div className="shrink-0 space-y-1">
        <h1 className="text-base sm:text-lg lg:text-xl font-bold text-gray-900 font-montserrat">
          Bitácora de documentos
        </h1>
        <p className="text-xs sm:text-sm text-gray-500">
          Consulte el estado de los documentos que ha cargado, agrupados por proveedor.
        </p>
      </div>

      <div className="shrink-0">
        <PayerSummaryCard summary={summary} isLoading={isLoading} variant="banner" />
      </div>

      <PayerDocumentHistory payerId={user?.entityId} canInactivate onInactivated={reload} />
    </div>
  );
};

export default PayerDocumentLog;
