import React, { useState } from "react";
import Swal from "sweetalert2";
import { operatorService } from "../../services/operator/operatorService";
import { formatMoney, formatDate, downloadBlob } from "../../utils/format";
import { apiErrorMessage, readApiErrorMessage, showError } from "../../utils/errors";
import escapeHtml from "../../utils/escapeHtml";
import ConfirmBatchModal from "./control-terminal/ConfirmBatchModal";
import RequestDetailModal from "./control-terminal/RequestDetailModal";
import PayerSummaryPanel from "./control-terminal/PayerSummaryPanel";
import RequestsPanel from "./control-terminal/RequestsPanel";
import useControlTerminalData from "./control-terminal/useControlTerminalData";
import { requestKey, todayIso } from "./control-terminal/requestUtils";

const ControlTerminal = () => {
  const {
    payersResume,
    selectedPayerId,
    setSelectedPayerId,
    currentPayer,
    isLoading,
    requests,
    isLoadingRequests,
    loadData,
  } = useControlTerminalData();

  const [generatingKey, setGeneratingKey] = useState(null);
  const [isConfirming, setIsConfirming] = useState(false);
  const [isConfirmModalOpen, setConfirmModalOpen] = useState(false);
  const [selectedBatchToConfirm, setSelectedBatchToConfirm] = useState(null);

  const [detailRequest, setDetailRequest] = useState(null);
  const [detailSuppliers, setDetailSuppliers] = useState([]);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);

  const handleGenerateBatch = async (request) => {
    if (!selectedPayerId) return;

    const confirm = await Swal.fire({
      title: "¿Generar lote de desembolso?",
      html: `Se generará un lote con <b>${escapeHtml(request.documentCount)}</b> documento(s) del pagador <b>${escapeHtml(currentPayer.name)}</b> (vencimiento <b>${escapeHtml(formatDate(request.dueDate))}</b>, solicitud <b>${escapeHtml(formatDate(request.requestDate))}</b>) por <b>$${escapeHtml(formatMoney(request.totalAmount))}</b>.<br/><br/>Fecha de desembolso: <b>${escapeHtml(formatDate(request.disbursementDate))}</b>.`,
      icon: "question",
      showCancelButton: true,
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#94a3b8",
      confirmButtonText: "Sí, generar",
      cancelButtonText: "Cancelar",
    });
    if (!confirm.isConfirmed) return;

    setGeneratingKey(requestKey(request));
    try {
      const safeName = (currentPayer.name || "Pagador").replace(/[^a-zA-Z0-9]/g, "_");
      const originalFileName = `Lote_${safeName}_D${request.disbursementDate}_V${request.dueDate}_S${request.requestDate}`;

      const pdf = await operatorService.generateBatch({
        payerId: selectedPayerId,
        originalFileName,
        groups: [
          {
            dueDate: request.dueDate,
            requestDate: request.requestDate,
            disbursementDate: request.disbursementDate,
          },
        ],
      });

      downloadBlob(pdf, `Reporte_Desembolso_${originalFileName}.pdf`);

      await Swal.fire(
        "¡Lote generado!",
        "Se descargó el reporte PDF del lote. Confirme el desembolso cuando el core bancario lo procese.",
        "success",
      );
      loadData();
    } catch (error) {
      console.error("Error generando lote de desembolso:", error);
      showError(await readApiErrorMessage(error, "No se pudo generar el lote."));
      loadData();
    } finally {
      setGeneratingKey(null);
    }
  };

  const handleOpenConfirmModal = async (request) => {
    try {
      const batch = await operatorService.getBatchDetails(request.batchId);
      setSelectedBatchToConfirm(batch || null);
      setConfirmModalOpen(true);
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo cargar el detalle del lote."));
    }
  };

  const handleExecuteConfirmation = async () => {
    if (!selectedBatchToConfirm?.id) return;

    const totalDocs = selectedBatchToConfirm.documents?.length || 0;
    const disbursementDate = selectedBatchToConfirm.disbursementDate;
    const isEarly = Boolean(disbursementDate) && disbursementDate > todayIso();
    const earlyWarning = isEarly
      ? `<div style="margin-top:12px;padding:10px;border-radius:8px;background:#fef3c7;color:#92400e;font-size:0.9em;">Este lote está programado para desembolsarse el <b>${escapeHtml(formatDate(disbursementDate))}</b>. Está confirmando el desembolso antes de esa fecha.</div>`
      : "";

    const confirm = await Swal.fire({
      title: isEarly ? "Confirmación antes de la fecha de desembolso" : "Confirmar lote completo",
      html: `Se marcarán como desembolsados los <b>${escapeHtml(totalDocs)}</b> documento(s) del lote <b>${escapeHtml(selectedBatchToConfirm.batchNumber)}</b>. Esta acción no se puede deshacer.${earlyWarning}`,
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#94a3b8",
      confirmButtonText: "Sí, confirmar",
      cancelButtonText: "Revisar",
    });
    if (!confirm.isConfirmed) return;

    setIsConfirming(true);
    try {
      await operatorService.confirmBatch(selectedBatchToConfirm.id);

      setConfirmModalOpen(false);
      setSelectedBatchToConfirm(null);

      Swal.fire("¡Lote confirmado!", "Todos los documentos del lote fueron marcados como desembolsados.", "success");
      loadData();
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo procesar la confirmación."));
    } finally {
      setIsConfirming(false);
    }
  };

  const handleOpenDetail = async (request) => {
    setDetailRequest(request);
    setDetailSuppliers([]);
    setIsLoadingDetail(true);
    try {
      const params = request.batchId
        ? { batchId: request.batchId }
        : {
            dueDate: request.dueDate,
            requestDate: request.requestDate,
            disbursementDate: request.disbursementDate,
          };
      const suppliers = await operatorService.getRequestSuppliers(selectedPayerId, params);
      setDetailSuppliers(suppliers);
    } catch (error) {
      setDetailRequest(null);
      showError(apiErrorMessage(error, "No se pudo cargar el detalle de la solicitud."));
    } finally {
      setIsLoadingDetail(false);
    }
  };

  return (
    <div className="w-full font-montserrat text-slate-800 h-[calc(100dvh-5.5rem)] min-h-0 overflow-hidden flex flex-col pb-4">
      <div className="shrink-0 mb-3 sm:mb-4">
        <h1 className="text-base sm:text-lg lg:text-xl font-bold text-slate-900">Terminal de desembolsos</h1>
        <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
          Genere lotes de desembolso por solicitud y confirme resultados del core bancario.
        </p>
      </div>

      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-3 sm:gap-4 lg:gap-6 lg:items-stretch overflow-y-auto lg:overflow-hidden">
        <PayerSummaryPanel
          payers={payersResume}
          selectedPayerId={selectedPayerId}
          onSelectPayer={setSelectedPayerId}
          currentPayer={currentPayer}
          isLoading={isLoading}
        />

        <RequestsPanel
          requests={requests}
          isLoading={isLoadingRequests}
          generatingKey={generatingKey}
          onGenerate={handleGenerateBatch}
          onConfirm={handleOpenConfirmModal}
          onDetail={handleOpenDetail}
        />
      </div>

      <ConfirmBatchModal
        key={`${selectedBatchToConfirm?.id || "none"}-${isConfirmModalOpen}`}
        isOpen={isConfirmModalOpen}
        onClose={() => setConfirmModalOpen(false)}
        onConfirm={handleExecuteConfirmation}
        batchData={selectedBatchToConfirm}
        isConfirming={isConfirming}
      />

      <RequestDetailModal
        request={detailRequest}
        payerName={currentPayer.name}
        suppliers={detailSuppliers}
        isLoading={isLoadingDetail}
        onClose={() => setDetailRequest(null)}
      />
    </div>
  );
};

export default ControlTerminal;
