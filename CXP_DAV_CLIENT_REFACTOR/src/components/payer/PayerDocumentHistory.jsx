import React, { useState, useMemo, useEffect, useCallback } from "react";
import Swal from "sweetalert2";
import { FaSearch, FaTimes } from "react-icons/fa";
import Table from "../Table";
import SearchInput from "../SearchInput";
import StatusBadge from "../StatusBadge";
import { documentService } from "../../services/core/documentService.js";
import useServerPagination from "../../hooks/useServerPagination.js";
import { DOCUMENT_BADGE_CLASS, DOCUMENT_STATUS, DOCUMENT_STATUSES } from "../../constants/status.js";
import { formatDate, formatMoney } from "../../utils/format.js";
import { apiErrorMessage, showError } from "../../utils/errors.js";
import escapeHtml from "../../utils/escapeHtml.js";

const ALL = "all";
const ITEMS_PER_PAGE = 10;

const formatDocumentDate = (value) => formatDate(value, "Pendiente");

const formatAmount = (value) => `$ ${formatMoney(value)}`;

const BASE_COLUMNS = [
  { header: "Número de documento", accessor: "documentNumber" },
  {
    header: "Proveedor",
    accessor: "supplierName",
    render: (value, row) => (
      <span className="flex flex-col">
        <span>{value || "Sin nombre"}</span>
        {row.supplierNit && <span className="text-[10px] text-gray-400">NIT {row.supplierNit}</span>}
      </span>
    ),
  },
  { header: "Emisión", accessor: "issueDate", render: formatDocumentDate },
  { header: "Vencimiento", accessor: "dueDate", render: formatDocumentDate },
  { header: "Desembolso", accessor: "disbursementDate", render: formatDocumentDate },
  { header: "Monto", accessor: "amount", render: formatAmount },
  { header: "Cargado por", accessor: "uploadedBy", render: (value) => value || "—" },
  {
    header: "Estado",
    accessor: "status",
    render: (status) => <StatusBadge status={status} statusMap={DOCUMENT_STATUS} className={DOCUMENT_BADGE_CLASS} />,
  },
];

const buildActionsColumn = ({ onInactivate, inactivatingId }) => ({
  header: "Acciones",
  accessor: "inactivatable",
  render: (inactivatable, row) =>
    inactivatable ? (
      <button
        type="button"
        onClick={() => onInactivate(row)}
        disabled={inactivatingId !== null}
        title="El proveedor ya no podrá solicitar este documento"
        className="inline-flex items-center px-2.5 py-1 rounded-md text-[11px] font-semibold text-gray-700 bg-gray-100 hover:bg-gray-200 border border-gray-200 transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {inactivatingId === row.id ? "Inactivando…" : "Pasar a Inactivo"}
      </button>
    ) : (
      <span className="text-gray-300">—</span>
    ),
});

/** Agrupa las filas del resumen (proveedor + estado) en una tarjeta por proveedor. */
const buildSupplierSummaries = (rows) => {
  const bySupplier = new Map();

  for (const row of rows) {
    if (!bySupplier.has(row.supplierId)) {
      bySupplier.set(row.supplierId, {
        key: row.supplierId,
        name: row.supplierName || "Sin nombre",
        nit: row.supplierNit,
        total: 0,
        amount: 0,
        counts: {},
      });
    }
    const summary = bySupplier.get(row.supplierId);
    const count = Number(row.count) || 0;
    summary.total += count;
    summary.amount += Number(row.amount) || 0;
    summary.counts[row.status] = (summary.counts[row.status] || 0) + count;
  }

  return [...bySupplier.values()].sort((a, b) => a.name.localeCompare(b.name, "es"));
};

const showLoadError = () => showError("No se pudo cargar la bitácora de documentos");

const sumCounts = (summaries) =>
  summaries.reduce((acc, summary) => {
    for (const [status, count] of Object.entries(summary.counts)) {
      acc[status] = (acc[status] || 0) + count;
    }
    return acc;
  }, {});

const StatusBreakdown = ({ counts }) => (
  <div className="flex flex-wrap gap-1.5 mt-2">
    {DOCUMENT_STATUSES.filter((s) => counts[s.id]).map((s) => (
      <span
        key={s.id}
        title={s.label}
        className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-semibold ${s.className}`}
      >
        <span className={`w-1.5 h-1.5 rounded-full ${s.dot}`} />
        {s.label}: {counts[s.id]}
      </span>
    ))}
  </div>
);

/**
 * Bitácora de documentos de un pagador agrupada por proveedor. Con {@code canInactivate} muestra la
 * columna Acciones; {@code onInactivated} se llama después de inactivar un documento.
 */
const PayerDocumentHistory = ({ payerId, canInactivate = false, onInactivated }) => {
  const [summaryRows, setSummaryRows] = useState([]);
  const [isLoadingSummary, setIsLoadingSummary] = useState(false);
  const [selectedSupplier, setSelectedSupplier] = useState(ALL);
  const [supplierSearch, setSupplierSearch] = useState("");
  const [activeStatus, setActiveStatus] = useState(ALL);
  const [inactivatingId, setInactivatingId] = useState(null);
  const [summaryVersion, setSummaryVersion] = useState(0);

  useEffect(() => {
    if (!payerId) return undefined;

    let cancelled = false;
    setIsLoadingSummary(true);

    documentService
      .getHistorySummaryByPayer(payerId)
      .then((rows) => {
        if (!cancelled) setSummaryRows(Array.isArray(rows) ? rows : []);
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("Error loading payer document summary:", error);
        setSummaryRows([]);
        showLoadError();
      })
      .finally(() => {
        if (!cancelled) setIsLoadingSummary(false);
      });

    return () => {
      cancelled = true;
    };
  }, [payerId, summaryVersion]);

  const fetchDocuments = useCallback(
    ({ search, page }) =>
      documentService.getHistoryByPayer(payerId, {
        supplierId: selectedSupplier === ALL ? undefined : selectedSupplier,
        status: activeStatus === ALL ? undefined : activeStatus,
        search,
        page,
        size: ITEMS_PER_PAGE,
      }),
    [payerId, selectedSupplier, activeStatus],
  );

  const {
    items: documents,
    totalPages,
    isLoading,
    searchTerm,
    setSearchTerm,
    currentPage,
    setCurrentPage,
    reload: reloadDocuments,
  } = useServerPagination(payerId ? fetchDocuments : null, { onError: showLoadError });

  const handleInactivate = useCallback(
    async (doc) => {
      const confirm = await Swal.fire({
        title: "¿Pasar documento a Inactivo?",
        html: `El documento <b>${escapeHtml(doc.documentNumber)}</b> de <b>${escapeHtml(doc.supplierName || "el proveedor")}</b> dejará de estar disponible para que el proveedor lo solicite y su monto se liberará del cupo. Esta acción no se puede deshacer.`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#dc2626",
        cancelButtonColor: "#94a3b8",
        confirmButtonText: "Sí, inactivar",
        cancelButtonText: "Cancelar",
      });
      if (!confirm.isConfirmed) return;

      setInactivatingId(doc.id);
      try {
        await documentService.inactivateDocument(doc.id);
        Swal.fire("Documento inactivado", "El documento pasó a estado Inactivo.", "success");
        setSummaryVersion((version) => version + 1);
        reloadDocuments();
        onInactivated?.();
      } catch (error) {
        showError(apiErrorMessage(error, "No se pudo inactivar el documento."));
      } finally {
        setInactivatingId(null);
      }
    },
    [reloadDocuments, onInactivated],
  );

  const columns = useMemo(
    () =>
      canInactivate
        ? [...BASE_COLUMNS, buildActionsColumn({ onInactivate: handleInactivate, inactivatingId })]
        : BASE_COLUMNS,
    [canInactivate, handleInactivate, inactivatingId],
  );

  const supplierSummaries = useMemo(() => buildSupplierSummaries(summaryRows), [summaryRows]);

  const overallCounts = useMemo(() => sumCounts(supplierSummaries), [supplierSummaries]);

  const overallTotal = useMemo(
    () => supplierSummaries.reduce((sum, summary) => sum + summary.total, 0),
    [supplierSummaries],
  );

  const visibleSuppliers = useMemo(() => {
    const term = supplierSearch.trim().toLowerCase();
    if (!term) return supplierSummaries;
    return supplierSummaries.filter(
      (s) => s.name.toLowerCase().includes(term) || (s.nit || "").toLowerCase().includes(term),
    );
  }, [supplierSummaries, supplierSearch]);

  const selectedSummary = supplierSummaries.find((s) => s.key === selectedSupplier);

  const statusCountsForSelection = selectedSummary ? selectedSummary.counts : overallCounts;
  const totalForSelection = selectedSummary ? selectedSummary.total : overallTotal;

  const selectSupplier = (key) => {
    setSelectedSupplier(key);
    setActiveStatus(ALL);
    setCurrentPage(1);
  };

  const supplierCardClass = (active) =>
    `w-full text-left p-3 rounded-lg border transition-all cursor-pointer ${
      active ? "bg-red-50 border-red-300 shadow-sm" : "bg-white border-gray-200 hover:bg-gray-50"
    }`;

  return (
    <div className="flex flex-col lg:flex-row flex-1 min-h-0 gap-4">
      {/* --- PROVEEDORES --- */}
      <aside className="lg:w-80 shrink-0 flex flex-col min-h-0 bg-white border border-gray-200 rounded-lg shadow-sm p-3 font-montserrat">
        <h2 className="text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
          Proveedores ({supplierSummaries.length})
        </h2>

        <div className="relative mb-3">
          <input
            type="text"
            placeholder="Buscar proveedor o NIT"
            value={supplierSearch}
            onChange={(e) => setSupplierSearch(e.target.value)}
            className="w-full pl-9 pr-3 py-2 border border-gray-200 rounded-md bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-red-300 text-xs"
          />
          <FaSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-xs pointer-events-none" />
        </div>

        <div className="flex-1 min-h-0 max-h-80 lg:max-h-none overflow-y-auto space-y-2 pr-1">
          <button
            type="button"
            onClick={() => selectSupplier(ALL)}
            className={supplierCardClass(selectedSupplier === ALL)}
          >
            <div className="flex justify-between items-baseline gap-2">
              <span className="text-sm font-bold text-gray-900">Todos los proveedores</span>
              <span className="text-xs text-gray-500 shrink-0">{overallTotal} docs</span>
            </div>
            <StatusBreakdown counts={overallCounts} />
          </button>

          {isLoadingSummary && <p className="text-xs text-gray-400 text-center py-4">Cargando proveedores…</p>}

          {!isLoadingSummary && visibleSuppliers.length === 0 && supplierSummaries.length > 0 && (
            <p className="text-xs text-gray-400 text-center py-4">
              Ningún proveedor coincide con &quot;{supplierSearch}&quot;.
            </p>
          )}

          {visibleSuppliers.map((supplier) => (
            <button
              key={supplier.key}
              type="button"
              onClick={() => selectSupplier(supplier.key)}
              className={supplierCardClass(selectedSupplier === supplier.key)}
            >
              <div className="flex justify-between items-baseline gap-2">
                <span className="text-sm font-bold text-gray-900 truncate">{supplier.name}</span>
                <span className="text-xs text-gray-500 shrink-0">{supplier.total} docs</span>
              </div>
              <div className="flex justify-between text-[11px] text-gray-500 mt-0.5">
                <span>{supplier.nit ? `NIT ${supplier.nit}` : "Sin NIT"}</span>
                <span className="font-semibold text-gray-700">{formatAmount(supplier.amount)}</span>
              </div>
              <StatusBreakdown counts={supplier.counts} />
            </button>
          ))}
        </div>
      </aside>

      {/* --- DOCUMENTOS --- */}
      <section className="flex-1 min-w-0 min-h-0 flex flex-col gap-3">
        {selectedSummary && (
          <div className="shrink-0 flex flex-wrap items-center justify-between gap-2 bg-white border border-gray-200 rounded-lg px-4 py-3 shadow-sm">
            <div>
              <p className="text-sm font-bold text-gray-900">{selectedSummary.name}</p>
              <p className="text-xs text-gray-500">
                {selectedSummary.nit ? `NIT ${selectedSummary.nit} · ` : ""}
                {selectedSummary.total} documentos · {formatAmount(selectedSummary.amount)}
              </p>
            </div>
            <button
              type="button"
              onClick={() => selectSupplier(ALL)}
              className="inline-flex items-center gap-1.5 text-xs font-medium text-gray-600 hover:text-red-600 cursor-pointer"
            >
              <FaTimes /> Ver todos
            </button>
          </div>
        )}

        <div className="shrink-0 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <SearchInput
            value={searchTerm}
            onChange={setSearchTerm}
            placeholder="Buscar por documento o proveedor"
            className="w-full sm:w-72 md:w-80 shrink-0 shadow-sm rounded-lg"
          />

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <label htmlFor="status-filter" className="text-xs font-medium text-gray-600 font-montserrat shrink-0">
              Estado
            </label>
            <select
              id="status-filter"
              value={activeStatus}
              onChange={(e) => {
                setActiveStatus(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full sm:w-56 px-3 py-2 border border-gray-200 rounded-md bg-white shadow-sm text-xs font-montserrat text-gray-700 focus:outline-none focus:ring-2 focus:ring-red-300 cursor-pointer"
            >
              {[{ id: ALL, label: "Todos" }, ...DOCUMENT_STATUSES].map((status) => {
                const count = status.id === ALL ? totalForSelection : statusCountsForSelection[status.id] || 0;
                return (
                  <option key={status.id} value={status.id}>
                    {status.label} ({count})
                  </option>
                );
              })}
            </select>
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
      </section>
    </div>
  );
};

export default PayerDocumentHistory;
