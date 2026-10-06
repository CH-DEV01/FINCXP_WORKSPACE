import React, { useEffect, useRef, useState } from "react";
import Swal from "sweetalert2";
import { HiOutlineDocumentText } from "react-icons/hi";
import { registerDocumentBatchService } from "../../services/core/registerDocumentBatchService.js";
import TermsAcceptanceModal from "../terms/TermsAcceptanceModal.jsx";
import UploadResources from "./UploadResources.jsx";
import { downloadBlob } from "../../utils/format.js";
import { readApiErrorMessage } from "../../utils/errors.js";

const baseName = (file) => file.name.replace(/\.[^/.]+$/, "");

/**
 * Centro de carga del Excel de documentos para un pagador.
 * Lo usan el administrador (elige el pagador en su panel) y el pagador (su propia entidad).
 */
const DocumentUploadCenter = ({ payerId, payerName, isLineFull = false, onUploaded }) => {
  const fileInputRef = useRef(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [acceptedTermVersion, setAcceptedTermVersion] = useState(null);
  const isAccepted = acceptedTermVersion != null;
  const [selectedFile, setSelectedFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadStatus, setUploadStatus] = useState("");
  const [uploadStatusType, setUploadStatusType] = useState("");
  const [settings, setSettings] = useState(null);

  useEffect(() => {
    registerDocumentBatchService
      .getUploadSettings()
      .then(setSettings)
      .catch((error) => console.error("No se pudo obtener la configuración de carga:", error));
  }, []);

  const allowedExtensions = settings?.allowedExtensions ?? [];
  const extensionsLabel = allowedExtensions.join(", ");

  const canSelectFile = Boolean(settings) && Boolean(payerId) && !isUploading && !isLineFull;

  const rejectFile = (event, title, text) => {
    Swal.fire({ icon: "error", title, text, confirmButtonText: "Entendido" });
    setSelectedFile(null);
    event.target.value = null;
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (!file) return;
    setUploadStatus("");

    const name = file.name.toLowerCase();
    if (!allowedExtensions.some((extension) => name.endsWith(extension))) {
      rejectFile(event, "Formato de archivo no válido", `Seleccione un archivo con formato ${extensionsLabel}.`);
      return;
    }
    if (file.size > settings.maxFileSizeMb * 1024 * 1024) {
      rejectFile(event, "Archivo demasiado grande", `El tamaño máximo permitido es ${settings.maxFileSizeMb} MB.`);
      return;
    }
    setSelectedFile(file);
  };

  const handleAcceptTerms = (version) => {
    setAcceptedTermVersion(version);
    setIsModalOpen(false);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setUploadStatus("Por favor selecciona un archivo primero");
      setUploadStatusType("error");
      return;
    }
    if (!payerId) {
      setUploadStatus("No hay un pagador seleccionado para la carga.");
      setUploadStatusType("error");
      return;
    }

    setIsUploading(true);
    setUploadStatus("Procesando archivo...");
    setUploadStatusType("info");

    try {
      const receipt = await registerDocumentBatchService.uploadBatch(selectedFile, payerId, acceptedTermVersion?.id);
      downloadBlob(receipt, `Comprobante_Exito_${baseName(selectedFile)}.pdf`);

      setUploadStatus("¡Lote cargado exitosamente!");
      setUploadStatusType("success");
      setSelectedFile(null);
      setAcceptedTermVersion(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      onUploaded?.();

      Swal.fire({
        title: "¡Lote cargado exitosamente!",
        text: "El archivo ha sido procesado correctamente. Se ha descargado automáticamente su comprobante oficial en PDF.",
        icon: "success",
        confirmButtonColor: "#059669",
        confirmButtonText: "Entendido",
      });
    } catch (error) {
      console.error("Error al subir archivo:", error);
      setUploadStatusType("error");

      if (error.response?.data instanceof Blob && error.response.data.type === "application/pdf") {
        downloadBlob(error.response.data, `Reporte_Errores_${baseName(selectedFile)}.pdf`);

        Swal.fire({
          title: "Carga rechazada por inconsistencias",
          text: "El archivo no pudo procesarse debido a errores en los datos o superación de límites. Se ha descargado un reporte PDF en su equipo con el detalle de las observaciones.",
          icon: "warning",
          confirmButtonColor: "#dc2626",
          confirmButtonText: "Revisar reporte",
        });

        setUploadStatus("Carga rechazada. Revise el reporte de errores descargado.");
      } else {
        const errorMessage = await readApiErrorMessage(
          error,
          "Error de conexión al procesar el archivo. Intente nuevamente.",
        );

        Swal.fire({
          title: "Error de Servidor",
          text: errorMessage,
          icon: "error",
          confirmButtonColor: "#dc2626",
        });

        setUploadStatus("Fallo de comunicación con el servidor.");
      }

      setAcceptedTermVersion(null);
      setIsModalOpen(false);
    } finally {
      setIsUploading(false);
    }
  };

  return (
    <div className="flex-1 font-montserrat flex flex-col min-h-[70vh] md:min-h-0">
      <div className="bg-white rounded-2xl shadow-lg flex-1 flex flex-col justify-between border-2 border-gray-200">
        <div className="w-full max-w-5xl mx-auto px-4 md:px-8 py-8 mt-4 md:mt-8">
          <div className="text-center mb-8">
            <h1 className="text-2xl md:text-3xl font-bold text-gray-800">Centro de carga de datos</h1>
            <p className="text-sm md:text-base text-gray-600 mt-2 pb-4">
              Gestión de archivos para el financiamiento de cuentas por pagar
            </p>
            {payerName && (
              <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-red-50 text-red-700 border border-red-100">
                Pagador: {payerName}
              </span>
            )}
          </div>

          <div className="flex justify-center mt-8 pb-8">
            <div className="w-full max-w-4xl h-0.5 bg-gradient-to-r from-transparent via-red-600 to-transparent opacity-70"></div>
          </div>

          <div className="w-full max-w-xl mx-auto">
            <UploadResources />

            <div className="flex flex-col">
              <h3 className="text-sm font-bold text-gray-700 mb-3 uppercase tracking-wider flex items-center">
                <HiOutlineDocumentText className="w-5 h-5 mr-2 text-red-600" />
                Carga de archivo
              </h3>

              <div className="border-2 border-dashed border-gray-300 rounded-xl p-8 hover:border-red-300 transition duration-200 bg-gray-50/50 flex-1 flex flex-col justify-center">
                <div className="text-center mb-6">
                  <svg
                    className="mx-auto h-12 w-12 text-gray-400"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                    />
                  </svg>
                  {settings && (
                    <p className="mt-2 text-xs text-gray-500">
                      Formatos soportados: {extensionsLabel} · Máximo {settings.maxFileSizeMb} MB y{" "}
                      {settings.maxRows.toLocaleString()} registros
                    </p>
                  )}
                </div>

                <div className="flex justify-center w-full">
                  <input
                    ref={fileInputRef}
                    id="file-upload"
                    type="file"
                    onChange={handleFileChange}
                    accept={allowedExtensions.join(",")}
                    className="sr-only"
                    disabled={!canSelectFile}
                  />
                  <label
                    htmlFor="file-upload"
                    className={`w-full text-center px-4 py-3 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 transition-colors ${
                      canSelectFile ? "cursor-pointer" : "opacity-50 cursor-not-allowed"
                    }`}
                  >
                    Seleccionar archivo
                  </label>
                </div>

                {selectedFile && (
                  <div className="mt-4 p-3 bg-red-50 border border-red-100 rounded-md flex justify-between items-center">
                    <span className="font-medium text-red-900 truncate max-w-[70%] text-sm">{selectedFile.name}</span>
                    <span className="text-red-700 text-xs font-bold">{(selectedFile.size / 1024).toFixed(2)} KB</span>
                  </div>
                )}
              </div>

              <div className="mt-6 space-y-3">
                <button
                  className={`w-full rounded-md px-4 py-2.5 text-sm font-medium transition-colors border flex items-center justify-center ${
                    isLineFull || selectedFile == null
                      ? "bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed"
                      : "bg-white text-red-600 border-red-200 hover:bg-red-50 cursor-pointer"
                  }`}
                  onClick={() => setIsModalOpen(true)}
                  disabled={isLineFull || selectedFile == null}
                >
                  <HiOutlineDocumentText className="h-4 w-4 mr-2 shrink-0" />
                  {isAccepted
                    ? `Términos aceptados (versión ${acceptedTermVersion.versionNumber})`
                    : "Ver términos y condiciones"}
                </button>

                <button
                  onClick={handleUpload}
                  disabled={isUploading || !isAccepted}
                  className={`w-full py-3 rounded-md shadow-md text-sm font-bold text-white transition-all flex justify-center items-center ${
                    !isAccepted || isUploading
                      ? "bg-gray-400 cursor-not-allowed"
                      : "bg-red-600 hover:bg-red-700 shadow-red-600/30"
                  }`}
                >
                  {isUploading ? (
                    <>
                      <svg
                        className="animate-spin -ml-1 mr-3 h-5 w-5"
                        xmlns="http://www.w3.org/2000/svg"
                        fill="none"
                        viewBox="0 0 24 24"
                      >
                        <circle
                          className="opacity-25"
                          cx="12"
                          cy="12"
                          r="10"
                          stroke="currentColor"
                          strokeWidth="4"
                        ></circle>
                        <path
                          className="opacity-75"
                          fill="currentColor"
                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                        ></path>
                      </svg>
                      <span>Procesando...</span>
                    </>
                  ) : (
                    "Procesar archivo"
                  )}
                </button>
                {uploadStatus && (
                  <div
                    className={`mt-2 p-3 rounded-md text-sm text-center font-medium border ${
                      uploadStatusType === "error"
                        ? "bg-red-100 text-red-700 border-red-200"
                        : uploadStatusType === "success"
                          ? "bg-green-50 text-green-700 border-green-200"
                          : "bg-blue-50 text-blue-700 border-blue-200"
                    }`}
                  >
                    {uploadStatus}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="bg-gray-50 px-4 md:px-8 py-4 border-t border-gray-200 mt-8 rounded-b-2xl">
          <p className="text-xs text-gray-500 text-center md:text-left">
            Última actualización: {new Date().toLocaleDateString()} | Versión 1.0.0
          </p>
        </div>
      </div>

      <TermsAcceptanceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        termTypeCode="PAYER_TERM_TYPE"
        confirmLabel="Aceptar términos"
        onConfirm={handleAcceptTerms}
      />
    </div>
  );
};

export default DocumentUploadCenter;
