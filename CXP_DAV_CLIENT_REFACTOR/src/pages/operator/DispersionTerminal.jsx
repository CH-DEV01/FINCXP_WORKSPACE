import React, { useRef, useState } from "react";
import Swal from "sweetalert2";
import { dispersionService } from "../../services/operator/dispersionService";
import { formatMoney, formatDate, downloadBlob } from "../../utils/format";
import { apiErrorMessage, readApiErrorMessage, showError } from "../../utils/errors";
import escapeHtml from "../../utils/escapeHtml";
import DispersionPayerPanel from "./dispersion-terminal/DispersionPayerPanel";
import DispersionRequestsPanel from "./dispersion-terminal/DispersionRequestsPanel";
import DispersionRequestModal from "./dispersion-terminal/DispersionRequestModal";
import useDispersionTerminalData from "./dispersion-terminal/useDispersionTerminalData";
import { dispersionRequestKey, safeFileSegment } from "./dispersion-terminal/dispersionUtils";
import { todayIso } from "./control-terminal/requestUtils";

const letterFileName = (batchNumber) => `Solicitud_Dispersion_${batchNumber}.pdf`;

const DispersionTerminal = () => {
  const {
    payers,
    selectedPayerId,
    setSelectedPayerId,
    currentPayer,
    isLoading,
    requests,
    isLoadingRequests,
    loadData,
  } = useDispersionTerminalData();

  const [openRequest, setOpenRequest] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [batchDetail, setBatchDetail] = useState(null);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [downloadingKey, setDownloadingKey] = useState(null);
  const openKeyRef = useRef(null);

  const closeModal = () => {
    openKeyRef.current = null;
    setOpenRequest(null);
  };

  const handleOpen = async (request) => {
    const key = dispersionRequestKey(request);
    openKeyRef.current = key;
    setOpenRequest(request);
    setDocuments([]);
    setBatchDetail(null);
    setIsLoadingDetail(true);
    try {
      if (request.batchId) {
        const detail = await dispersionService.getBatchDetails(request.batchId);
        if (openKeyRef.current !== key) return;
        setBatchDetail(detail);
        setDocuments(detail?.documents ?? []);
      } else {
        const pending = await dispersionService.getPendingDocuments(selectedPayerId, request.dueDate);
        if (openKeyRef.current !== key) return;
        setDocuments(pending);
      }
    } catch (error) {
      if (openKeyRef.current !== key) return;
      closeModal();
      showError(apiErrorMessage(error, "No se pudo cargar el detalle de la solicitud."));
    } finally {
      if (openKeyRef.current === key) setIsLoadingDetail(false);
    }
  };

  const handleGenerate = async () => {
    if (!openRequest || !selectedPayerId) return;
    const total = documents.reduce((sum, d) => sum + Number(d.nominalAmount || 0), 0);

    const confirm = await Swal.fire({
      title: "¿Generar lote de dispersión?",
      html: `Se generará un lote con <b>${escapeHtml(documents.length)}</b> documento(s) del pagador <b>${escapeHtml(currentPayer.name)}</b> con vencimiento <b>${escapeHtml(formatDate(openRequest.dueDate))}</b> por <b>$${escapeHtml(formatMoney(total))}</b>.<br/><br/>Se dispersará en la misma fecha de vencimiento.`,
      icon: "question",
      showCancelButton: true,
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#94a3b8",
      confirmButtonText: "Sí, generar",
      cancelButtonText: "Cancelar",
    });
    if (!confirm.isConfirmed) return;

    setIsSubmitting(true);
    try {
      const pdf = await dispersionService.generateBatch({
        payerId: selectedPayerId,
        dueDate: openRequest.dueDate,
      });
      downloadBlob(
        pdf,
        `Solicitud_Dispersion_${safeFileSegment(currentPayer.name, "Pagador")}_V${openRequest.dueDate}.pdf`,
      );
      closeModal();
      await Swal.fire(
        "¡Lote generado!",
        "Se descargó la solicitud de dispersión. Confirme la dispersión cuando el core bancario la procese.",
        "success",
      );
    } catch (error) {
      console.error("Error generando lote de dispersión:", error);
      showError(await readApiErrorMessage(error, "No se pudo generar el lote de dispersión."));
    } finally {
      setIsSubmitting(false);
      loadData();
    }
  };

  const handleConfirm = async () => {
    const batch = batchDetail?.batch;
    if (!batch?.id) return;

    const isEarly = Boolean(batch.dispersionDate) && batch.dispersionDate > todayIso();
    const earlyWarning = isEarly
      ? `<div style="margin-top:12px;padding:10px;border-radius:8px;background:#fef3c7;color:#92400e;font-size:0.9em;">Este lote está programado para dispersarse el <b>${escapeHtml(formatDate(batch.dispersionDate))}</b>. Está confirmando la dispersión antes de esa fecha.</div>`
      : "";

    const confirm = await Swal.fire({
      title: isEarly ? "Confirmación antes de la fecha de dispersión" : "Confirmar dispersión",
      html: `Se marcarán como dispersados los <b>${escapeHtml(documents.length)}</b> documento(s) del lote <b>${escapeHtml(batch.batchNumber)}</b>. Esta acción no se puede deshacer.${earlyWarning}`,
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#94a3b8",
      confirmButtonText: "Sí, confirmar",
      cancelButtonText: "Revisar",
    });
    if (!confirm.isConfirmed) return;

    setIsSubmitting(true);
    try {
      await dispersionService.confirmBatch(batch.id);
      closeModal();
      Swal.fire(
        "¡Dispersión confirmada!",
        "Todos los documentos del lote fueron marcados como dispersados.",
        "success",
      );
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudo confirmar la dispersión."));
    } finally {
      setIsSubmitting(false);
      loadData();
    }
  };

  const handleDownload = async (request) => {
    setDownloadingKey(dispersionRequestKey(request));
    try {
      const pdf = await dispersionService.redownloadBatch(request.batchId);
      downloadBlob(pdf, letterFileName(request.batchNumber));
    } catch (error) {
      showError(await readApiErrorMessage(error, "No se pudo descargar la solicitud de dispersión."));
    } finally {
      setDownloadingKey(null);
    }
  };

  const modalAccountNumber = openRequest?.batchId ? batchDetail?.batch?.payerAccountNumber : currentPayer.accountNumber;

  return (
    <div className="w-full font-montserrat text-slate-800 h-[calc(100dvh-5.5rem)] min-h-0 overflow-hidden flex flex-col pb-4">
      <div className="shrink-0 mb-3 sm:mb-4">
        <h1 className="text-base sm:text-lg lg:text-xl font-bold text-slate-900">Terminal de dispersiones</h1>
        <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
          Genere lotes de dispersión por fecha de vencimiento con los documentos que no fueron anticipados y confirme su
          dispersión.
        </p>
      </div>

      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-3 sm:gap-4 lg:gap-6 lg:items-stretch overflow-y-auto lg:overflow-hidden">
        <DispersionPayerPanel
          payers={payers}
          selectedPayerId={selectedPayerId}
          onSelectPayer={setSelectedPayerId}
          currentPayer={currentPayer}
          isLoading={isLoading}
        />

        <DispersionRequestsPanel
          requests={requests}
          isLoading={isLoadingRequests}
          downloadingKey={downloadingKey}
          onOpen={handleOpen}
          onDownload={handleDownload}
        />
      </div>

      <DispersionRequestModal
        request={openRequest}
        payerName={currentPayer.name}
        payerAccountNumber={modalAccountNumber}
        documents={documents}
        signerName={batchDetail?.signerName}
        isLoading={isLoadingDetail}
        isSubmitting={isSubmitting}
        onGenerate={handleGenerate}
        onConfirm={handleConfirm}
        onClose={closeModal}
      />
    </div>
  );
};

export default DispersionTerminal;
