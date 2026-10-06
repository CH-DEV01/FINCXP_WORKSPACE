import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Swal from "sweetalert2";
import { masterAgreementService } from "../../../services/admin/masterAgreementService.js";
import { documentService } from "../../../services/core/documentService.js";
import { fundingRequestService } from "../../../services/core/fundingRequestService.js";
import { apiErrorMessage, showError } from "../../../utils/errors.js";
import { EMPTY_CALCULATION, mapDocumentToAccount, parseLocalDate, toCalculation } from "./documentSelection";

const CALCULATION_DELAY_MS = 400;

/**
 * Documentos financiables del convenio, su selección, la próxima fecha de desembolso y la
 * simulación de costo de la selección (con retardo para no consultar en cada clic).
 */
const useDocumentSelection = (masterAgreementId) => {
  const [calculation, setCalculation] = useState(EMPTY_CALCULATION);
  const [currentPage, setCurrentPage] = useState(1);
  const [disbursementDate, setDisbursementDate] = useState(null);
  const [cutoffTime, setCutoffTime] = useState(null);
  const [accountsPayable, setAccountsPayable] = useState([]);
  const [isLoadingDocuments, setIsLoadingDocuments] = useState(false);
  const calculationRequestRef = useRef(0);
  const calculationTimerRef = useRef(null);

  const selectedIds = useMemo(
    () => accountsPayable.filter((item) => item.checked).map((item) => item.id),
    [accountsPayable],
  );

  useEffect(() => {
    if (!masterAgreementId) return undefined;

    let cancelled = false;

    masterAgreementService
      .getNextDisbursementDate(masterAgreementId)
      .then((data) => {
        if (cancelled) return;
        setDisbursementDate(parseLocalDate(data?.nextDisbursementDate));
        setCutoffTime(data?.cutoffTime || null);
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("Error al obtener la próxima fecha de desembolso del backend:", error);
        setDisbursementDate(null);
        setCutoffTime(null);
      });

    return () => {
      cancelled = true;
    };
  }, [masterAgreementId]);

  const fetchFinanceableDocuments = useCallback(async () => {
    if (!masterAgreementId) return;

    setIsLoadingDocuments(true);
    try {
      const documents = await documentService.getFinanceableByMasterAgreement(masterAgreementId);

      setAccountsPayable(Array.isArray(documents) ? documents.map(mapDocumentToAccount) : []);
      setCurrentPage(1);
    } catch (error) {
      console.error("Error loading financeable documents:", error);
      setAccountsPayable([]);
      showError("No se pudieron cargar los documentos del convenio");
    } finally {
      setIsLoadingDocuments(false);
    }
  }, [masterAgreementId]);

  useEffect(() => {
    fetchFinanceableDocuments();
  }, [fetchFinanceableDocuments]);

  /** Solo se aplica la respuesta de la última simulación enviada. */
  const calculateCost = useCallback(
    async (documentIds) => {
      const requestId = ++calculationRequestRef.current;

      if (documentIds.length === 0) {
        setCalculation(EMPTY_CALCULATION);
        return;
      }

      if (!masterAgreementId) {
        console.warn("No se puede simular el cálculo: falta masterAgreementId");
        return;
      }

      try {
        const data = await fundingRequestService.calculateCost({
          masterAgreementId,
          documentIds,
        });
        if (requestId !== calculationRequestRef.current) return;

        setCalculation(toCalculation(data));
        if (data?.disbursementDate) {
          setDisbursementDate(parseLocalDate(data.disbursementDate));
        }
      } catch (apiError) {
        if (requestId !== calculationRequestRef.current) return;
        console.error("Error en API calcular intereses:", apiError);
        setCalculation(EMPTY_CALCULATION);

        if (apiError?.response?.status === 422) {
          await Swal.fire({
            title: "Documentos no disponibles",
            text: apiErrorMessage(apiError, "Algunos documentos ya no están disponibles."),
            icon: "warning",
            confirmButtonText: "Actualizar lista",
          });
          fetchFinanceableDocuments();
        }
      }
    },
    [masterAgreementId, fetchFinanceableDocuments],
  );

  useEffect(() => {
    if (selectedIds.length === 0) {
      calculateCost(selectedIds);
      return undefined;
    }

    calculationTimerRef.current = setTimeout(() => {
      calculationTimerRef.current = null;
      calculateCost(selectedIds);
    }, CALCULATION_DELAY_MS);

    return () => clearTimeout(calculationTimerRef.current);
  }, [selectedIds, calculateCost]);

  /** Cancela la simulación pendiente y la ejecuta de inmediato con la selección actual. */
  const calculateNow = useCallback(() => {
    clearTimeout(calculationTimerRef.current);
    return calculateCost(selectedIds);
  }, [calculateCost, selectedIds]);

  const toggleDocument = useCallback((itemId) => {
    setAccountsPayable((prev) => prev.map((item) => (item.id === itemId ? { ...item, checked: !item.checked } : item)));
  }, []);

  return {
    accountsPayable,
    isLoadingDocuments,
    currentPage,
    setCurrentPage,
    selectedIds,
    toggleDocument,
    disbursementDate,
    cutoffTime,
    calculation,
    calculateNow,
    refreshDocuments: fetchFinanceableDocuments,
  };
};

export default useDocumentSelection;
