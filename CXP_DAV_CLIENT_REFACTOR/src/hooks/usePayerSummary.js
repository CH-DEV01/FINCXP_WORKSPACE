import { useCallback, useEffect, useState } from "react";
import { OWN_PAYER, payerService } from "../services/core/payerService.js";

/**
 * Resumen de un pagador: sin argumento, el del pagador autenticado; con un id, el de ese pagador
 * (operador bancario); con null no consulta nada. {@code reload} vuelve a consultarlo, por ejemplo
 * después de una carga o una inactivación que cambian el consumo de la línea.
 */
const usePayerSummary = (payerId = OWN_PAYER) => {
  const [summary, setSummary] = useState(null);
  const [isLoading, setIsLoading] = useState(Boolean(payerId));
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!payerId) {
      setSummary(null);
      setIsLoading(false);
      return undefined;
    }

    let cancelled = false;
    setIsLoading(true);
    payerService
      .getSummary(payerId)
      .then((data) => {
        if (!cancelled) setSummary(data ?? null);
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("No se pudo obtener el resumen del pagador:", error);
        setSummary(null);
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [payerId, version]);

  const reload = useCallback(() => setVersion((current) => current + 1), []);

  const creditLine = summary?.creditLine ?? null;
  const isLineFull = creditLine ? Number(creditLine.availableToUpload) <= 0 : false;

  return { summary, isLoading, isLineFull, reload };
};

export default usePayerSummary;
