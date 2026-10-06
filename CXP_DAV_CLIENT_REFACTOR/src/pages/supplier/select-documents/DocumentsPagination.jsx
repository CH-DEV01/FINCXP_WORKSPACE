import React from "react";

const DocumentsPagination = ({ currentPage, totalPages, totalItems, pageSize, onPageChange }) => (
  <div className="flex-shrink-0 flex justify-between px-3 sm:px-4 py-3 bg-white border-t border-gray-200">
    <div className="flex-1 flex justify-between sm:hidden">
      <button
        onClick={() => onPageChange(Math.max(1, currentPage - 1))}
        disabled={currentPage === 1}
        className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
      >
        Anterior
      </button>
      <button
        onClick={() => onPageChange(Math.min(totalPages, currentPage + 1))}
        disabled={currentPage === totalPages || totalPages === 0}
        className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50"
      >
        Siguiente
      </button>
    </div>
    <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between gap-2">
      <div className="min-w-0">
        <p className="text-sm text-gray-700">
          Mostrando <span className="font-medium">{totalItems === 0 ? 0 : (currentPage - 1) * pageSize + 1}</span> a{" "}
          <span className="font-medium">{Math.min(currentPage * pageSize, totalItems)}</span> de{" "}
          <span className="font-medium">{totalItems}</span> resultados
        </p>
      </div>
      <div className="overflow-x-auto max-w-full">
        <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px">
          <button
            onClick={() => onPageChange(currentPage - 1)}
            disabled={currentPage === 1}
            className={`relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium ${currentPage === 1 ? "text-gray-300 cursor-not-allowed" : "text-gray-500 hover:bg-gray-50"}`}
          >
            <span className="sr-only">Anterior</span>
          </button>
          {Array.from({ length: totalPages }, (_, i) => i + 1).map((number) => (
            <button
              key={number}
              onClick={() => onPageChange(number)}
              className={`relative inline-flex items-center px-3 lg:px-4 py-2 border text-sm font-medium ${currentPage === number ? "z-10 bg-red-50 border-red-500 text-red-600" : "bg-white border-gray-300 text-gray-500 hover:bg-gray-50"}`}
            >
              {number}
            </button>
          ))}
          <button
            onClick={() => onPageChange(currentPage + 1)}
            disabled={currentPage === totalPages || totalPages === 0}
            className={`relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium ${currentPage === totalPages || totalPages === 0 ? "text-gray-300 cursor-not-allowed" : "text-gray-500 hover:bg-gray-50"}`}
          >
            <span className="sr-only">Siguiente</span>
          </button>
        </nav>
      </div>
    </div>
  </div>
);

export default DocumentsPagination;
