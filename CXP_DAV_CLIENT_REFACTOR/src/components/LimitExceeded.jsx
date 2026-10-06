import React from "react";
import { HiExclamationTriangle } from "react-icons/hi2";

/**
 * Componente de disclaimer horizontal para notificar que se ha excedido el límite de crédito.
 */
const LimitExceeded = () => {
  return (
    <div
      className="flex items-center justify-center p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg shadow-sm w-full mx-auto"
      role="alert"
    >
      {/* Icono de advertencia */}
      <HiExclamationTriangle className="h-6 w-6 mr-3 flex-shrink-0" aria-hidden="true" />

      {/* Mensaje principal */}
      <p className="font-medium text-sm md:text-base">
        Límite de crédito excedido. No es posible cargar más documentos.
      </p>
    </div>
  );
};

export default LimitExceeded;
