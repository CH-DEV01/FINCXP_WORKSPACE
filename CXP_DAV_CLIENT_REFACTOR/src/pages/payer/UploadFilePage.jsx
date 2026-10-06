import React from "react";
import { useAuth } from "../../context/AuthContext.jsx";
import DocumentUploadCenter from "../../components/upload/DocumentUploadCenter.jsx";
import LimitExceeded from "../../components/LimitExceeded.jsx";
import PayerSummaryCard from "../../components/payer/PayerSummaryCard.jsx";
import usePayerSummary from "../../hooks/usePayerSummary.js";

const UploadFilePage = () => {
  const { userData } = useAuth();
  const { summary, isLoading, isLineFull, reload } = usePayerSummary();

  return (
    <div className="flex flex-col min-h-screen gap-4 pb-6">
      {isLineFull && <LimitExceeded />}

      <div className="flex flex-col md:flex-row flex-1 gap-4">
        <aside className="w-full md:w-1/3 lg:w-1/4 shrink-0 flex flex-col">
          <PayerSummaryCard summary={summary} isLoading={isLoading} variant="panel" />
        </aside>

        <DocumentUploadCenter payerId={userData?.entityId} isLineFull={isLineFull} onUploaded={reload} />
      </div>
    </div>
  );
};

export default UploadFilePage;
