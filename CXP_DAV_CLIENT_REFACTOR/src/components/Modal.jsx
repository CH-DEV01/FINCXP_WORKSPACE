import React, { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { FaTimes } from "react-icons/fa";

const Modal = ({ isOpen, onClose, title, children, footer }) => {
  const [isAnimating, setIsAnimating] = useState(false);

  useEffect(() => {
    if (isOpen) {
      const timer = setTimeout(() => setIsAnimating(true), 10);
      return () => clearTimeout(timer);
    } else {
      setIsAnimating(false);
    }
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  return createPortal(
    <div
      className={`
                fixed inset-0 z-50 
                flex items-center justify-center 
                bg-black/70 
                transition-opacity duration-300
                ${isAnimating ? "opacity-100" : "opacity-0"}
            `}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className={`
                    rounded-lg
                    bg-white  shadow-xl 
                    w-full max-w-lg m-4
                    flex flex-col 
                    max-h-[90vh]
                    font-montserrat
                    transition-all duration-300 ease-out
                    ${isAnimating ? "opacity-100 scale-100" : "opacity-0 scale-95"}
                `}
      >
        <div className="flex justify-between items-center p-4 border-b border-1 border-gray-200 rounded-lg">
          <h3 className="uppercase text-xs font-semibold font-montserrat text-gray-600">{title}</h3>
          <button
            onClick={onClose}
            className="
                            hover:cursor-pointer
                            font-montserrat
                            p-1 rounded-full 
                            text-gray-400 hover:text-red-600 hover:bg-gray-100 
                            transition-colors
                        "
            title="Cerrar"
          >
            <FaTimes size={20} />
          </button>
        </div>

        <div className="p-6 overflow-y-auto">{children}</div>

        {footer && (
          <div className="flex justify-end gap-3 p-4 bg-gray-50 border-t border-1 border-gray-200 rounded-b-lg">
            {footer}
          </div>
        )}
      </div>
    </div>,
    document.body,
  );
};

export default Modal;
