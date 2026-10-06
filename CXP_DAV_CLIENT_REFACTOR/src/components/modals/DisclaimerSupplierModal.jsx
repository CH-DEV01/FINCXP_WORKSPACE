// src/components/Modal.jsx
import React, { useEffect, useState } from "react";
import { createPortal } from "react-dom";

const DisclaimerSupplierModal = ({ isOpen, onClose, children }) => {
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    if (isOpen) {
      // Un pequeño retraso para que la transición CSS se aplique después de que el DOM se renderice
      const timer = setTimeout(() => setIsAnimating(true), 10);
      return () => clearTimeout(timer);
    } else {
      setIsAnimating(false);
    }
  }, [isOpen]);

  // Cierra el modal al presionar la tecla "Escape"
  useEffect(() => {
    const handleEsc = (event) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    if (isOpen) {
      window.addEventListener("keydown", handleEsc);
    }
    return () => {
      window.removeEventListener("keydown", handleEsc);
    };
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  // Usamos un Portal para renderizar el modal en el body y evitar problemas de z-index
  return createPortal(
    // Overlay: un fondo oscuro que cubre toda la pantalla
    <div className="fixed inset-0 bg-black/70 flex justify-center items-end sm:items-center z-50 p-0 sm:p-4">
      {/* Contenido del modal: usamos stopPropagation para que al hacer clic no se cierre */}
      <div
        className={`bg-white rounded-t-2xl sm:rounded-lg shadow-xl p-4 sm:p-6 relative w-full max-w-4xl m-0 sm:m-4
                            max-h-[92vh] sm:max-h-[90vh]
                            overflow-y-auto
                            flex flex-col
                            transition-all duration-300 ease-out
                            ${isAnimating ? "opacity-100 scale-100" : "opacity-0 scale-95"}`}
        // Detenemos la propagación para que no se cierre si tuviera un onClick en el padre
        onClick={(e) => e.stopPropagation()}
      >
        {/* Contenido dinámico que se pasa al componente */}
        {children}

        {/* Botón de cierre */}
        <button
          onClick={onClose}
          className="absolute top-2 right-2 p-1 rounded-full text-gray-500 hover:bg-gray-200 hover:text-gray-800"
          aria-label="Cerrar modal"
        >
          <svg
            className="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
          </svg>
        </button>
      </div>
    </div>,
    document.body,
  );
};

export default DisclaimerSupplierModal;
