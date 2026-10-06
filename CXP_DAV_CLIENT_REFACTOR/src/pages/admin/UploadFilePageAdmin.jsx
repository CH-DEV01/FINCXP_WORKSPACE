import React, { useState, useEffect } from "react";
import LimitExceeded from "../../components/LimitExceeded.jsx";
import PayerData from "../../components/PayerData.jsx";
import PayerDirectory from "../../components/payer/PayerDirectory.jsx";
import DocumentUploadCenter from "../../components/upload/DocumentUploadCenter.jsx";
import { entityService } from "../../services/admin/entityService.js";
import { creditFacilityService } from "../../services/admin/creditFacilityService.js";
import { formatMoney } from "../../utils/format.js";

const toCreditMetrics = (facility) => ({
  totalLimit: Number(facility.facilityLimitAmount) || 0,
  currentConsumed: Number(facility.amountInUse) || 0,
  availableLimit: Number(facility.availableAmount) || 0,
  utilizationRate: Number(facility.utilizationPercentage) || 0,
});

const UploadFilePageAdmin = () => {
  const [payers, setPayers] = useState([]);
  const [selectedPayer, setSelectedPayer] = useState({});
  const [isLoadingPayers, setIsLoadingPayers] = useState(true);
  const [creditMetrics, setCreditMetrics] = useState(null);
  const [metricsVersion, setMetricsVersion] = useState(0);

  useEffect(() => {
    const fetchPayers = async () => {
      try {
        const page = await entityService.getPayers(0, 100);
        const realPayers = page?.content ?? [];

        setPayers(realPayers);

        if (realPayers.length > 0) {
          setSelectedPayer(realPayers[0]);
        }
      } catch (error) {
        console.error("Error fetching payers:", error);
      } finally {
        setIsLoadingPayers(false);
      }
    };
    fetchPayers();
  }, []);

  const selectedPayerId = selectedPayer?.id;

  useEffect(() => {
    if (!selectedPayerId) return undefined;
    let cancelled = false;
    setCreditMetrics(null);
    creditFacilityService
      .getPayerCreditLineDetails(selectedPayerId)
      .then((facility) => {
        if (!cancelled) setCreditMetrics(facility ? toCreditMetrics(facility) : null);
      })
      .catch(() => {
        if (!cancelled) setCreditMetrics(null);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedPayerId, metricsVersion]);

  const isLineFull = creditMetrics ? creditMetrics.availableLimit <= 0 : false;

  return (
    <div className="flex flex-col min-h-screen gap-4">
      {isLineFull && (
        <div className="w-full">
          <LimitExceeded />
        </div>
      )}

      <div className="flex flex-col md:flex-row flex-1 gap-4 pb-6">
        {/* --- PANEL IZQUIERDO (Rojo) --- */}
        <div className="shadow-2xl w-full md:w-1/3 lg:w-1/4 bg-gradient-to-r from-red-600 to-red-800 bg-red-800 text-white p-4 rounded-xl font-montserrat flex flex-col">
          <div className="flex-shrink-0">
            <PayerData payer={selectedPayer} />
          </div>

          {creditMetrics && (
            <div className="bg-red-900/40 p-4 rounded-xl mt-4 border border-red-500/30 backdrop-blur-sm shadow-inner flex-shrink-0">
              <h4 className="text-[10px] uppercase font-bold text-red-200 mb-3 tracking-wider flex items-center gap-2">
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                  ></path>
                </svg>
                Estado de la Línea
              </h4>

              <div className="space-y-3">
                <div className="flex justify-between items-center border-b border-red-700/50 pb-2">
                  <span className="text-xs text-red-100 font-medium">Cupo Total</span>
                  <span className="text-sm font-mono font-bold">${formatMoney(creditMetrics.totalLimit)}</span>
                </div>
                <div className="flex justify-between items-center border-b border-red-700/50 pb-2">
                  <span className="text-xs text-red-100 font-medium">Consumo actual</span>
                  <span className="text-sm font-mono font-bold">${formatMoney(creditMetrics.currentConsumed)}</span>
                </div>
                <div className="flex justify-between items-center bg-red-950/50 -mx-2 px-2 py-2 rounded-lg">
                  <span className="text-xs text-white font-bold">Neto Disponible</span>
                  <span className="text-sm font-mono font-bold text-emerald-400">
                    ${formatMoney(creditMetrics.availableLimit)}
                  </span>
                </div>
              </div>

              <div className="mt-4 pt-3 border-t border-red-700/50">
                <div className="flex justify-between text-[10px] font-bold text-red-200 mb-1.5 uppercase tracking-wider">
                  <span>Ocupación</span>
                  <span>{creditMetrics.utilizationRate}%</span>
                </div>
                <div className="w-full bg-red-950/80 h-2.5 rounded-full overflow-hidden shadow-inner">
                  <div
                    className={`h-full rounded-full transition-all duration-1000 ${creditMetrics.utilizationRate > 85 ? "bg-red-400" : "bg-emerald-400"}`}
                    style={{ width: `${Math.min(creditMetrics.utilizationRate, 100)}%` }}
                  ></div>
                </div>
              </div>
            </div>
          )}

          <div className="mt-6 flex flex-col flex-1 min-h-0">
            <PayerDirectory
              payers={payers}
              isLoading={isLoadingPayers}
              selectedPayerId={selectedPayerId}
              onSelect={setSelectedPayer}
            />
          </div>
        </div>

        <DocumentUploadCenter
          payerId={selectedPayerId}
          payerName={selectedPayer?.name}
          isLineFull={isLineFull}
          onUploaded={() => setMetricsVersion((version) => version + 1)}
        />
      </div>
    </div>
  );
};

export default UploadFilePageAdmin;
