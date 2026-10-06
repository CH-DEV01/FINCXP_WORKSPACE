import { useCallback, useEffect, useState } from "react";
import { dispersionService } from "../../../services/operator/dispersionService";
import { apiErrorMessage, showError } from "../../../utils/errors";

/** Pagadores y solicitudes de dispersión del pagador seleccionado. */
const useDispersionTerminalData = () => {
  const [payers, setPayers] = useState([]);
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
    dispersionService
      .getPayerRequests(selectedPayerId)
      .then((payerRequests) => {
        if (!cancelled) setRequests(payerRequests);
      })
      .catch((error) => {
        console.error("Error al cargar solicitudes de dispersión:", error);
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
      const loaded = await dispersionService.getPayers();
      setPayers(loaded);
      setSelectedPayerId((prev) => (prev && loaded.some((p) => p.id === prev) ? prev : loaded[0]?.id || ""));
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

  const currentPayer = payers.find((p) => p.id === selectedPayerId) || {
    name: isLoading ? "Cargando..." : "Sin pagadores",
    accountNumber: null,
    pendingGroups: 0,
  };

  return {
    payers,
    selectedPayerId,
    setSelectedPayerId,
    currentPayer,
    isLoading,
    requests,
    isLoadingRequests,
    loadData,
  };
};

export default useDispersionTerminalData;
