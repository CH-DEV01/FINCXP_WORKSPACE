import React, { useCallback, useEffect, useState } from "react";
import Icon from "@mdi/react";
import { mdiLoading, mdiDownload, mdiInboxOutline, mdiDomain, mdiHistory } from "@mdi/js";
import { operatorService } from "../../services/operator/operatorService";
import StatusBadge from "../../components/StatusBadge";
import useServerPagination from "../../hooks/useServerPagination";
import { REQUEST_BADGE_CLASS, REQUEST_STATUS } from "../../constants/status";
import { formatMoney, formatDate, formatDateTime, downloadBlob } from "../../utils/format";
import { apiErrorMessage, readApiErrorMessage, showError } from "../../utils/errors";

const toRequestStatus = (batchStatus) => (batchStatus === "SETTLED" ? "DESEMBOLSADO" : "EN_PROCESO");

const RequestStatusBadge = ({ status }) => (
  <StatusBadge status={status} statusMap={REQUEST_STATUS} className={REQUEST_BADGE_CLASS} />
);

const PAGE_SIZE = 10;

const showLoadError = (error) => showError(apiErrorMessage(error, "No se pudo cargar la bitácora de lotes."));

const BatchLog = () => {
  const [payers, setPayers] = useState([]);
  const [selectedPayerId, setSelectedPayerId] = useState("");
  const [downloadingId, setDownloadingId] = useState(null);

  useEffect(() => {
    operatorService
      .getPayersResume()
      .then(setPayers)
      .catch((error) => console.error("Error al cargar pagadores:", error));
  }, []);

  const fetchBatches = useCallback(
    ({ page }) => operatorService.getBatchHistory({ payerId: selectedPayerId || undefined, page, size: PAGE_SIZE }),
    [selectedPayerId],
  );

  const {
    items: batches,
    page: history,
    totalPages,
    totalElements,
    isLoading,
    currentPage,
    setCurrentPage,
  } = useServerPagination(fetchBatches, { onError: showLoadError });

  const totalAmountToDisburse = history.totalAmountToDisburse || 0;

  const handleDownload = async (batch) => {
    setDownloadingId(batch.id);
    try {
      const pdf = await operatorService.redownloadBatch(batch.id);
      downloadBlob(pdf, `Reporte_Desembolso_${batch.batchNumber}.pdf`);
    } catch (error) {
      showError(await readApiErrorMessage(error, "No se pudo descargar el reporte del lote."));
    } finally {
      setDownloadingId(null);
    }
  };

  const downloadButton = (batch) => (
    <button
      onClick={() => handleDownload(batch)}
      disabled={downloadingId !== null}
      className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-200 rounded-lg disabled:opacity-40"
      title="Descargar reporte PDF"
    >
      <Icon
        path={downloadingId === batch.id ? mdiLoading : mdiDownload}
        size={0.8}
        className={downloadingId === batch.id ? "animate-spin" : ""}
      />
    </button>
  );

  const stateBlock = isLoading ? (
    <div className="flex flex-col items-center justify-center text-slate-400 gap-2">
      <Icon path={mdiLoading} size={1.2} className="animate-spin" />
      <p className="text-sm font-medium text-slate-600">Cargando lotes...</p>
    </div>
  ) : (
    <div className="flex flex-col items-center justify-center text-slate-400 gap-2">
      <Icon path={mdiInboxOutline} size={1.6} />
      <p className="text-sm font-medium text-slate-600">No hay lotes generados</p>
    </div>
  );

  const showState = isLoading || batches.length === 0;

  return (
    <div className="w-full font-montserrat text-slate-800 h-[calc(100dvh-5.5rem)] min-h-0 overflow-hidden flex flex-col pb-4">
      <div className="shrink-0 mb-3 sm:mb-4 flex flex-col sm:flex-row sm:items-end sm:justify-between gap-3">
        <div className="min-w-0">
          <h1 className="text-base sm:text-lg lg:text-xl font-bold text-slate-900">Bitácora de lotes</h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Lotes de desembolso generados, su estado y los usuarios que los generaron y confirmaron.
          </p>
        </div>
        <div className="sm:w-72 shrink-0">
          <label className="flex items-center gap-2 text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1.5">
            <Icon path={mdiDomain} size={0.6} className="text-red-500" /> Pagador
          </label>
          <select
            value={selectedPayerId}
            onChange={(e) => {
              setSelectedPayerId(e.target.value);
              setCurrentPage(1);
            }}
            className="w-full bg-white border border-slate-200 rounded-xl text-sm p-2.5 text-slate-700 font-medium focus:ring-2 focus:ring-red-100 focus:border-red-500 outline-none transition-all cursor-pointer"
          >
            <option value="">Todos los pagadores</option>
            {payers.map((payer) => (
              <option key={payer.id} value={payer.id}>
                {payer.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="flex-1 min-h-0 bg-white rounded-2xl shadow-sm border border-slate-200/60 overflow-hidden flex flex-col">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-wrap items-center gap-x-4 gap-y-1 bg-slate-50/50 shrink-0">
          <div className="flex items-center gap-2 mr-auto">
            <Icon path={mdiHistory} size={0.9} className="text-slate-400" />
            <h2 className="text-xs sm:text-sm font-bold text-slate-700 uppercase tracking-wider">
              Lotes de desembolso
            </h2>
          </div>
          {!isLoading && totalElements > 0 && (
            <div className="flex gap-4 text-xs font-semibold text-slate-600">
              <span>{totalElements} lote(s)</span>
              <span className="font-mono text-slate-800">
                Total a desembolsar: ${formatMoney(totalAmountToDisburse)}
              </span>
            </div>
          )}
        </div>

        <div className="flex-1 min-h-0 overflow-auto">
          {/* Móvil: cards */}
          <div className="md:hidden p-3 space-y-3">
            {showState ? (
              <div className="py-16">{stateBlock}</div>
            ) : (
              batches.map((batch) => (
                <div key={batch.id} className="rounded-xl border border-slate-200 p-3 space-y-2 text-sm">
                  <div className="flex justify-between items-center gap-2">
                    <span className="font-bold text-slate-700 truncate">{batch.batchNumber}</span>
                    <RequestStatusBadge status={toRequestStatus(batch.status)} />
                  </div>
                  {[
                    ["Pagador", batch.payerName || "-"],
                    ["Desembolso", formatDate(batch.disbursementDate)],
                    ["Vencimiento", formatDate(batch.dueDate)],
                    ["Solicitud", formatDate(batch.requestDate)],
                    ["Generación", formatDateTime(batch.createdAt)],
                    ["Registros", batch.documentCount],
                    ["Generado por", batch.createdByName || "-"],
                    ["Confirmado por", batch.confirmedByName || "-"],
                  ].map(([label, value]) => (
                    <div key={label} className="flex justify-between gap-2">
                      <span className="text-slate-500">{label}</span>
                      <span className="font-medium text-slate-700 text-right break-words">{value}</span>
                    </div>
                  ))}
                  <div className="flex justify-between items-center gap-2 border-t border-slate-100 pt-2">
                    <span className="text-slate-500">Monto a desembolsar</span>
                    <span className="font-bold text-slate-800">${formatMoney(batch.totalAmountToDisburse)}</span>
                  </div>
                  <div className="flex justify-end">{downloadButton(batch)}</div>
                </div>
              ))
            )}
          </div>

          {/* Desktop: tabla */}
          <div className="hidden md:block min-w-full">
            <table className="w-full text-left border-collapse whitespace-nowrap">
              <thead className="sticky top-0 z-10 bg-white">
                <tr className="border-b border-slate-100 text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  <th className="px-4 py-3">Lote</th>
                  <th className="px-4 py-3">Pagador</th>
                  <th className="px-4 py-3">Desembolso</th>
                  <th className="px-4 py-3">Vencimiento</th>
                  <th className="px-4 py-3">Solicitud</th>
                  <th className="px-4 py-3">Fecha generación</th>
                  <th className="px-4 py-3 text-center">Registros</th>
                  <th className="px-4 py-3 text-right">Monto a desembolsar</th>
                  <th className="px-4 py-3 text-center">Estado</th>
                  <th className="px-4 py-3">Generado por</th>
                  <th className="px-4 py-3">Confirmado por</th>
                  <th className="px-4 py-3 text-center">PDF</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {showState ? (
                  <tr>
                    <td colSpan="12" className="px-5 py-16 text-center">
                      {stateBlock}
                    </td>
                  </tr>
                ) : (
                  batches.map((batch) => (
                    <tr key={batch.id} className="hover:bg-slate-50 transition-colors text-xs">
                      <td className="px-4 py-3 font-bold text-slate-700">{batch.batchNumber}</td>
                      <td className="px-4 py-3 font-medium text-slate-600">{batch.payerName || "-"}</td>
                      <td className="px-4 py-3 font-semibold text-slate-700">{formatDate(batch.disbursementDate)}</td>
                      <td className="px-4 py-3 text-slate-600">{formatDate(batch.dueDate)}</td>
                      <td className="px-4 py-3 text-slate-600">{formatDate(batch.requestDate)}</td>
                      <td className="px-4 py-3 text-slate-500">{formatDateTime(batch.createdAt)}</td>
                      <td className="px-4 py-3 text-center">
                        <span className="font-bold bg-slate-100 text-slate-600 px-2.5 py-1 rounded-md">
                          {batch.documentCount}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right text-sm font-bold text-slate-800">
                        ${formatMoney(batch.totalAmountToDisburse)}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <RequestStatusBadge status={toRequestStatus(batch.status)} />
                      </td>
                      <td className="px-4 py-3 text-slate-600">{batch.createdByName || "-"}</td>
                      <td className="px-4 py-3 text-slate-600">
                        {batch.confirmedByName || <span className="text-slate-300">—</span>}
                      </td>
                      <td className="px-4 py-3 text-center">{downloadButton(batch)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {totalPages > 1 && (
          <div className="shrink-0 border-t border-slate-100 px-4 py-3 flex items-center justify-between gap-3 text-xs text-slate-600">
            <span>
              Página <span className="font-semibold text-slate-800">{currentPage}</span> de{" "}
              <span className="font-semibold text-slate-800">{totalPages}</span>
            </span>
            <div className="flex gap-2">
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={isLoading || currentPage === 1}
                className="px-3 py-1.5 rounded-lg border border-slate-200 font-semibold hover:bg-slate-50 disabled:opacity-40"
              >
                Anterior
              </button>
              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={isLoading || currentPage >= totalPages}
                className="px-3 py-1.5 rounded-lg border border-slate-200 font-semibold hover:bg-slate-50 disabled:opacity-40"
              >
                Siguiente
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default BatchLog;
