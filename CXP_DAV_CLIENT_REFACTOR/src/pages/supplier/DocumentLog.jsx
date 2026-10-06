import React, { useState, useCallback } from "react";
import { FaBuilding } from "react-icons/fa";
import Table from "../../components/Table";
import SearchInput from "../../components/SearchInput";
import StatusBadge from "../../components/StatusBadge";
import { useAuth } from "../../context/AuthContext.jsx";
import { useAgreement } from "../../context/AgreementContext.jsx";
import { documentService } from "../../services/core/documentService.js";
import useServerPagination from "../../hooks/useServerPagination.js";
import {
  DOCUMENT_BADGE_CLASS,
  DOCUMENT_STATUS,
  DOCUMENT_STATUSES,
  SUPPLIER_HIDDEN_DOCUMENT_STATUS_IDS,
} from "../../constants/status.js";
import { formatDate, formatMoney } from "../../utils/format.js";
import { showError } from "../../utils/errors.js";

const formatDocumentDate = (value) => formatDate(value, "Pendiente");

const columns = [
  { header: "Número de documento", accessor: "documentNumber" },
  {
    header: "Fecha de emisión",
    accessor: "issueDate",
    render: formatDocumentDate,
  },
  {
    header: "Fecha de desembolso",
    accessor: "disbursementDate",
    render: formatDocumentDate,
  },
  {
    header: "Monto",
    accessor: "amount",
    render: (value) => `$ ${formatMoney(value)}`,
  },
  {
    header: "Monto abonado",
    accessor: "disbursedAmount",
    render: (value) =>
      value != null ? (
        <span className="font-semibold text-green-700">$ {formatMoney(value)}</span>
      ) : (
        <span className="text-gray-400">—</span>
      ),
  },
  {
    header: "Estado",
    accessor: "status",
    render: (status) => <StatusBadge status={status} statusMap={DOCUMENT_STATUS} className={DOCUMENT_BADGE_CLASS} />,
  },
];

const ALL = "all";

const filters = [
  { id: ALL, name: "Todos" },
  ...DOCUMENT_STATUSES.filter((status) => !SUPPLIER_HIDDEN_DOCUMENT_STATUS_IDS.includes(status.id)).map((status) => ({
    id: status.id,
    name: status.label,
  })),
];

const ITEMS_PER_PAGE = 10;

const showLoadError = () => showError("No se pudo cargar la bitácora de documentos");

const DocumentLog = () => {
  const { user } = useAuth();
  const { agreement } = useAgreement();

  const supplierId = user?.entityId;
  const payerId = agreement?.payerId;
  const payerName = agreement?.PayerName || agreement?.payerName || "N/A";

  const [activeFilter, setActiveFilter] = useState(ALL);

  const fetchDocuments = useCallback(
    ({ search, page }) =>
      documentService.getHistoryBySupplier(supplierId, {
        payerId,
        status: activeFilter === ALL ? undefined : activeFilter,
        search,
        page,
        size: ITEMS_PER_PAGE,
      }),
    [supplierId, payerId, activeFilter],
  );

  const {
    items: documents,
    totalPages,
    isLoading,
    searchTerm,
    setSearchTerm,
    currentPage,
    setCurrentPage,
  } = useServerPagination(supplierId ? fetchDocuments : null, { onError: showLoadError });

  return (
    <div className="flex flex-col w-full h-[calc(100dvh-5.5rem)] min-h-0 overflow-hidden gap-3 sm:gap-4">
      <div className="shrink-0 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="min-w-0 space-y-1">
          <h1 className="text-base sm:text-lg lg:text-xl font-bold text-gray-900 font-montserrat">
            Bitácora de documentos
          </h1>
          <p className="text-xs sm:text-sm text-gray-500">
            Consulte el historial y estado de sus documentos de financiamiento.
          </p>
        </div>

        <div className="w-fit sm:shrink-0 flex items-center gap-2.5 bg-white rounded-lg shadow-sm border border-gray-200 font-montserrat px-3 py-2.5 sm:px-4">
          <div className="shrink-0 flex items-center justify-center w-8 h-8 rounded-full bg-red-50 text-red-600 text-sm">
            <FaBuilding />
          </div>
          <div className="min-w-0">
            <p className="text-[10px] sm:text-xs font-semibold uppercase tracking-wide text-red-700">Pagador</p>
            <p className="text-xs sm:text-sm font-bold text-gray-900 break-words sm:whitespace-nowrap">{payerName}</p>
          </div>
        </div>
      </div>

      <div className="shrink-0 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <SearchInput
          value={searchTerm}
          onChange={setSearchTerm}
          placeholder="Buscar por número de documento"
          className="w-full sm:w-72 md:w-80 lg:max-w-md shrink-0 shadow-sm rounded-lg"
        />

        <div className="w-full sm:w-auto sm:ml-auto overflow-x-auto">
          <div className="flex w-max ml-auto items-center justify-end gap-2 pb-0.5">
            {filters.map((filter) => (
              <button
                key={filter.id}
                type="button"
                onClick={() => {
                  setActiveFilter(filter.id);
                  setCurrentPage(1);
                }}
                className={`
                  font-montserrat shrink-0
                  hover:cursor-pointer
                  py-1.5 px-3 rounded-full text-xs font-medium transition-all
                  ${
                    activeFilter === filter.id
                      ? "bg-red-600 text-white shadow-md"
                      : "bg-white text-gray-700 border border-gray-200 hover:bg-gray-50 shadow-sm"
                  }
                `}
              >
                {filter.name}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="flex-1 min-h-0 flex flex-col">
        <Table
          columns={columns}
          data={documents}
          isLoading={isLoading}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
          fillHeight
        />
      </div>
    </div>
  );
};

export default DocumentLog;
