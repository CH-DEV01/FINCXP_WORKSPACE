import React from "react";
import DocumentsPagination from "./DocumentsPagination";
import { formatDueDate, getDueInfo } from "./documentSelection";
import { formatMoney } from "../../../utils/format";

const LoadingSpinner = () => (
  <div className="flex justify-center items-center py-10">
    <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-red-700"></div>
  </div>
);

const NoDocumentsView = () => (
  <div className="flex flex-col items-center justify-center gap-4 text-gray-500 py-12">
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="w-16 h-16 text-gray-300"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      strokeWidth={1.5}
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
      />
    </svg>
    <h3 className="text-lg font-semibold text-gray-800">¡Todo está al día!</h3>
    <p className="text-sm max-w-xs text-center">
      No hay documentos pendientes de selección en este momento. ¡Buen trabajo!
    </p>
  </div>
);

const PayableDocumentsList = ({ documents, isLoading, currentPage, pageSize, onPageChange, onToggle }) => {
  const totalPages = Math.ceil(documents.length / pageSize);
  const currentItems = documents.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const handleChange = (itemId) => (e) => {
    e.stopPropagation();
    onToggle(itemId);
  };

  return (
    <div className="flex flex-col h-[520px] sm:h-[600px] lg:h-full lg:min-h-0 rounded-lg shadow-lg overflow-hidden bg-white">
      <div className="flex-1 overflow-auto">
        {isLoading ? (
          <LoadingSpinner />
        ) : documents.length > 0 ? (
          <>
            {/* Vista móvil: cards */}
            <div className="md:hidden p-3 space-y-3">
              {currentItems.map((item) => {
                const { issueDate, dueDate, isDisabled } = getDueInfo(item);

                return (
                  <label
                    key={item.id}
                    className={`
                      block rounded-lg border p-3 transition
                      ${item.checked && !isDisabled ? "bg-red-50 border-red-700" : "border-gray-200 bg-white"}
                      ${isDisabled ? "bg-gray-100 opacity-60" : "active:bg-gray-50"}
                    `}
                  >
                    <div className="flex items-start gap-3">
                      <input
                        type="checkbox"
                        checked={item.checked && !isDisabled}
                        disabled={isDisabled}
                        onChange={handleChange(item.id)}
                        className={`mt-0.5 h-4 w-4 rounded border-gray-300 text-red-950 focus:ring-red-950 shrink-0 ${isDisabled ? "cursor-not-allowed pointer-events-none" : ""}`}
                        style={{ accentColor: "#8B0000" }}
                      />
                      <div className="min-w-0 flex-1 space-y-1.5">
                        <div className="flex justify-between gap-2">
                          <span className="text-xs text-gray-500">Documento</span>
                          <span
                            className={`text-sm font-bold truncate ${isDisabled ? "text-gray-400" : "text-red-950"}`}
                          >
                            {item.documentNumber}
                          </span>
                        </div>
                        <div className="flex justify-between gap-2 text-sm">
                          <span className="text-gray-500">Emisión</span>
                          <span className="text-gray-700">{issueDate.toLocaleDateString("es-ES")}</span>
                        </div>
                        <div className="flex justify-between gap-2 text-sm">
                          <span className="text-gray-500">Vencimiento</span>
                          <span className={isDisabled ? "text-red-600 font-semibold" : "text-gray-700"}>
                            {formatDueDate(dueDate)}
                          </span>
                        </div>
                        <div className="flex justify-between gap-2 text-sm border-t border-gray-100 pt-1.5">
                          <span className="text-gray-500">Monto</span>
                          <span className="font-semibold text-gray-900">$ {formatMoney(item.amount)}</span>
                        </div>
                      </div>
                    </div>
                  </label>
                );
              })}
            </div>

            {/* Vista desktop: tabla */}
            <div className="hidden md:block overflow-x-auto">
              <table className="min-w-[640px] w-full">
                <thead className="bg-gray-50 border-b border-gray-200 sticky top-0">
                  <tr>
                    <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-16">
                      Selección
                    </th>
                    <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Número de documento
                    </th>
                    <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Fecha Emisión
                    </th>
                    <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Fecha Vencimiento
                    </th>
                    <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Monto
                    </th>
                  </tr>
                </thead>

                <tbody className="divide-y divide-gray-200">
                  {currentItems.map((item) => {
                    const { issueDate, dueDate, isDisabled } = getDueInfo(item);

                    return (
                      <tr
                        key={item.id}
                        className={`
                                          ${item.checked ? "bg-red-50 border-l-4 border-red-700" : ""} 
                                          ${isDisabled ? "bg-gray-100 opacity-60 grayscale" : "hover:bg-gray-50"}
                                          `}
                      >
                        <td className="px-4 lg:px-6 py-4 whitespace-nowrap w-16">
                          <input
                            type="checkbox"
                            checked={item.checked && !isDisabled}
                            disabled={isDisabled}
                            onChange={handleChange(item.id)}
                            className={`h-4 w-4 rounded border-gray-300 text-red-950 focus:ring-red-950 ${isDisabled ? "cursor-not-allowed pointer-events-none" : ""}`}
                            style={{ accentColor: "#8B0000" }}
                          />
                        </td>

                        <td className="px-4 lg:px-6 py-4 whitespace-nowrap">
                          <span className={`font-bold text-sm ${isDisabled ? "text-gray-400" : "text-red-950"}`}>
                            {item.documentNumber}
                          </span>
                        </td>

                        <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {issueDate.toLocaleDateString("es-ES")}
                        </td>

                        <td
                          className={`px-4 lg:px-6 py-4 whitespace-nowrap text-sm ${isDisabled ? "text-red-600 font-semibold" : "text-gray-500"}`}
                        >
                          {formatDueDate(dueDate)}
                        </td>

                        <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                          $ {formatMoney(item.amount)}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </>
        ) : (
          <NoDocumentsView />
        )}
      </div>
      <DocumentsPagination
        currentPage={currentPage}
        totalPages={totalPages}
        totalItems={documents.length}
        pageSize={pageSize}
        onPageChange={onPageChange}
      />
    </div>
  );
};

export default PayableDocumentsList;
