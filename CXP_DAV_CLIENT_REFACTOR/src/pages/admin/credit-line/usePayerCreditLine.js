import { useCallback, useEffect, useState } from "react";
import { creditFacilityService } from "../../../services/admin/creditFacilityService";
import { entityService } from "../../../services/admin/entityService";
import { EMPTY_CREDIT_LINE, toCreditLine } from "./creditLineConfig";

const HISTORY_PAGE_SIZE = 10;

/** Pagadores, cupo del pagador activo y su historial paginado de movimientos. */
const usePayerCreditLine = () => {
  const [payersList, setPayersList] = useState([]);
  const [activePayer, setActivePayer] = useState("");
  const [isLoadingPayers, setIsLoadingPayers] = useState(true);

  const [creditLineData, setCreditLineData] = useState(null);
  const [creditLineVersion, setCreditLineVersion] = useState(0);
  const [repaymentHistory, setRepaymentHistory] = useState([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(1);
  const [historyReloadKey, setHistoryReloadKey] = useState(0);

  useEffect(() => {
    const fetchPayers = async () => {
      try {
        const page = await entityService.getPayers();
        const serverPayers = page?.content ?? [];

        setPayersList(serverPayers);

        if (serverPayers.length > 0) {
          setActivePayer(serverPayers[0].id);
        }
      } catch (error) {
        console.error("Error al cargar la lista de pagadores:", error);
        setPayersList([]);
      } finally {
        setIsLoadingPayers(false);
      }
    };

    fetchPayers();
  }, []);

  useEffect(() => {
    if (!activePayer) return undefined;

    let cancelled = false;

    creditFacilityService
      .getPayerCreditLineDetails(activePayer)
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
  }, [activePayer, creditLineVersion]);

  const selectPayer = useCallback((payerId) => {
    setActivePayer(payerId);
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
    payersList,
    activePayer,
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
