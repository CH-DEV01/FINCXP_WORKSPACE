import { useCallback, useEffect, useState } from "react";
import { creditFacilityService } from "../../../services/admin/creditFacilityService";
import { entityService } from "../../../services/admin/entityService";
import { EMPTY_CREDIT_LINE, toCreditLine } from "./creditLineConfig";

const HISTORY_PAGE_SIZE = 10;

/** Pagador activo (vía búsqueda en servidor), su cupo y el historial paginado de movimientos. */
const usePayerCreditLine = () => {
  const [selectedPayer, setSelectedPayer] = useState(null);
  const [isLoadingPayers, setIsLoadingPayers] = useState(true);

  const [creditLineData, setCreditLineData] = useState(null);
  const [creditLineVersion, setCreditLineVersion] = useState(0);
  const [repaymentHistory, setRepaymentHistory] = useState([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(1);
  const [historyReloadKey, setHistoryReloadKey] = useState(0);

  const activePayerId = selectedPayer?.id ?? "";

  useEffect(() => {
    let cancelled = false;
    entityService
      .getPayerSummaries({ page: 0, size: 1 })
      .then((page) => {
        if (cancelled) return;
        const first = page?.content?.[0] ?? null;
        setSelectedPayer(first);
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("Error al cargar la lista de pagadores:", error);
        setSelectedPayer(null);
      })
      .finally(() => {
        if (!cancelled) setIsLoadingPayers(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!activePayerId) return undefined;

    let cancelled = false;

    creditFacilityService
      .getPayerCreditLineDetails(activePayerId)
      .then((facility) => {
        if (!cancelled && facility) setCreditLineData(toCreditLine(facility));
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("Error general al obtener la línea de crédito:", error);
        setCreditLineData(EMPTY_CREDIT_LINE);
      });

    return () => {
      cancelled = true;
    };
  }, [activePayerId, creditLineVersion]);

  const selectPayer = useCallback((payer) => {
    setSelectedPayer(payer);
    setCreditLineData(null);
    setHistoryPage(0);
  }, []);

  const facilityId = creditLineData?.id;

  useEffect(() => {
    if (!facilityId) {
      setRepaymentHistory([]);
      setHistoryTotalPages(1);
      return undefined;
    }

    let cancelled = false;

    creditFacilityService
      .getHistory(facilityId, { page: historyPage, size: HISTORY_PAGE_SIZE })
      .then((page) => {
        if (cancelled) return;
        setRepaymentHistory(Array.isArray(page?.content) ? page.content : []);
        setHistoryTotalPages(Math.max(1, page?.totalPages || 1));
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("Error al cargar el historial:", error);
        setRepaymentHistory([]);
        setHistoryTotalPages(1);
      });

    return () => {
      cancelled = true;
    };
  }, [facilityId, historyPage, historyReloadKey]);

  /** Vuelve a leer el cupo y la primera página del historial (tras registrar un abono). */
  const refresh = useCallback(() => {
    setCreditLineVersion((version) => version + 1);
    setHistoryPage(0);
    setHistoryReloadKey((key) => key + 1);
  }, []);

  return {
    selectedPayer,
    activePayerId,
    isLoadingPayers,
    selectPayer,
    creditLineData,
    repaymentHistory,
    historyPage,
    setHistoryPage,
    historyTotalPages,
    refresh,
  };
};

export default usePayerCreditLine;
