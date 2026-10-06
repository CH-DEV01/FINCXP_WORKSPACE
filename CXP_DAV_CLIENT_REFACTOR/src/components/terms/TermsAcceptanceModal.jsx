import React, { useEffect, useState } from "react";
import DisclaimerSupplierModal from "../modals/DisclaimerSupplierModal.jsx";
import TermsMarkdown from "./TermsMarkdown.jsx";
import { termService } from "../../services/terms/termService.js";
import { formatDate } from "../../utils/format.js";
import { apiErrorMessage } from "../../utils/errors.js";

/**
 * Muestra la versión vigente de los términos del tipo indicado y exige marcar la casilla
 * de aceptación. Se consulta al abrir para que el usuario siempre vea el texto vigente;
 * onConfirm recibe la versión aceptada para registrar su id.
 */
const TermsAcceptanceModal = ({ isOpen, onClose, termTypeCode, onConfirm, confirmLabel = "Confirmar" }) => {
  const [version, setVersion] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState("");
  const [isAccepted, setIsAccepted] = useState(false);

  useEffect(() => {
    if (!isOpen) return undefined;

    let cancelled = false;
    setIsAccepted(false);
    setIsLoading(true);
    setLoadError("");

    termService
      .getActive(termTypeCode)
      .then((data) => {
        if (!cancelled) setVersion(data ?? null);
      })
      .catch((error) => {
        console.error("Error al cargar los términos y condiciones:", error);
        if (!cancelled) {
          setVersion(null);
          setLoadError(apiErrorMessage(error, "No se pudieron cargar los términos y condiciones. Intente nuevamente."));
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [isOpen, termTypeCode]);

  const handleConfirm = () => {
    if (!version || !isAccepted) return;
    onConfirm(version);
  };

  const publicationDate = formatDate(version?.publicationDate, null);

  return (
    <DisclaimerSupplierModal isOpen={isOpen} onClose={onClose}>
      {isLoading && (
        <div className="flex justify-center items-center py-16">
          <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-red-700"></div>
        </div>
      )}

      {!isLoading && loadError && (
        <div className="py-10 text-center">
          <p className="text-sm text-red-700 font-medium">{loadError}</p>
        </div>
      )}

      {!isLoading && version && (
        <>
          <div className="mb-4 border-b pb-2 pr-8">
            <h2 className="text-base sm:text-xl font-bold">{version.title}</h2>
            <p className="text-xs text-gray-500 mt-1">
              Versión {version.versionNumber}
              {publicationDate && ` · Vigente desde el ${publicationDate}`}
              {version.documentUrl && (
                <>
                  {" · "}
                  <a
                    href={version.documentUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-red-700 underline"
                  >
                    Ver documento
                  </a>
                </>
              )}
            </p>
          </div>

          <div className="mb-6 max-h-56 sm:max-h-72 overflow-y-auto pr-2 sm:pr-4">
            <TermsMarkdown content={version.content} />
          </div>

          <div className="flex items-start mb-6">
            <input
              id="terms-accept-checkbox"
              type="checkbox"
              checked={isAccepted}
              onChange={() => setIsAccepted((current) => !current)}
              className="cursor-pointer w-4 h-4 mt-1 text-red-600 bg-gray-100 border-gray-300 rounded focus:ring-red-500 shrink-0"
            />
            <label htmlFor="terms-accept-checkbox" className="ml-3 text-sm font-medium text-gray-900">
              {version.acceptanceText}
            </label>
          </div>
        </>
      )}

      <div className="flex flex-col-reverse sm:flex-row sm:justify-end gap-3 border-t pt-4">
        <button
          onClick={onClose}
          className="cursor-pointer w-full sm:w-auto px-5 py-2.5 sm:py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-semibold"
        >
          Cancelar
        </button>
        <button
          onClick={handleConfirm}
          disabled={!version || !isAccepted}
          className="cursor-pointer w-full sm:w-auto px-5 py-2.5 sm:py-2 bg-red-600 text-white rounded-lg font-semibold disabled:bg-gray-400 disabled:cursor-not-allowed hover:bg-red-700"
        >
          {confirmLabel}
        </button>
      </div>
    </DisclaimerSupplierModal>
  );
};

export default TermsAcceptanceModal;
