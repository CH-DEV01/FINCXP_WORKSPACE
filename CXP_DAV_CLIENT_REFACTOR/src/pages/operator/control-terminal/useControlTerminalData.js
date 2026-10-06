import { useCallback, useEffect, useState } from "react";
import { operatorService } from "../../../services/operator/operatorService";
import { apiErrorMessage, showError } from "../../../utils/errors";

/** Resumen de pagadores y solicitudes de desembolso del pagador seleccionado. */
const useControlTerminalData = () => {
  const [payersResume, setPayersResume] = useState([]);
  const [selectedPayerId, setSelectedPayerId] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const [requests, setRequests] = useState([]);
  const [isLoadingRequests, setIsLoadingRequests] = useState(false);
  const [requestsReloadToken, setRequestsReloadToken] = useState(0);

  useEffect(() => {
    if (!selectedPayerId) {
      setRequests([]);
      return;
    }

    let cancelled = false;
    setIsLoadingRequests(true);
    operatorService
      .getPayerRequests(selectedPayerId)
      .then((payerRequests) => {
        if (!cancelled) setRequests(payerRequests);
      })
      .catch((error) => {
        console.error("Error al cargar solicitudes de desembolso:", error);
        if (!cancelled) setRequests([]);
      })
      .finally(() => {
        if (!cancelled) setIsLoadingRequests(false);
      });

    return () => {
      cancelled = true;
    };
  }, [selectedPayerId, requestsReloadToken]);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    try {
      const payers = await operatorService.getPayersResume();
      setPayersResume(payers);
      setSelectedPayerId((prev) => (prev && payers.some((p) => p.id === prev) ? prev : payers[0]?.id || ""));
      setRequestsReloadToken((token) => token + 1);
    } catch (error) {
      console.error("Error al cargar datos iniciales:", error);
      showError(apiErrorMessage(error, "No se pudo cargar la información de la terminal."));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const currentPayer = payersResume.find((p) => p.id === selectedPayerId) || {
    name: isLoading ? "Cargando..." : "Sin pagadores",
    creditLineNumber: null,
    availableRequests: 0,
  };

  return {
    payersResume,
    selectedPayerId,
    setSelectedPayerId,
    currentPayer,
    isLoading,
    requests,
    isLoadingRequests,
    loadData,
  };
};

export default useControlTerminalData;
