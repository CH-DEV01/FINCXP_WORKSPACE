import React, { useEffect, useState } from "react";
import { HiOutlineBookOpen, HiOutlineDownload, HiOutlineFolderOpen } from "react-icons/hi";
import { PDF_MIME_TYPE, uploadResourceService, XLSX_MIME_TYPE } from "../../services/core/uploadResourceService.js";
import { downloadBlob } from "../../utils/format.js";
import { readApiErrorMessage, showError } from "../../utils/errors.js";

const ResourceItem = ({ icon, title, description, onDownload, available, busy }) => (
  <div className="flex items-center justify-between gap-3 rounded-lg border border-gray-200 bg-white px-4 py-3">
    <div className="flex items-center gap-3 min-w-0">
      <div className="bg-red-50 p-2 rounded-full shrink-0">{icon}</div>
      <div className="min-w-0">
        <p className="text-sm font-semibold text-gray-800">{title}</p>
        <p className="text-xs text-gray-500 truncate">{available ? description : "No disponible"}</p>
      </div>
    </div>
    <button
      type="button"
      onClick={onDownload}
      disabled={!available || busy}
      className="shrink-0 rounded-md border px-3 py-1.5 text-xs font-medium transition-colors border-red-200 bg-white text-red-600 hover:bg-red-50 cursor-pointer disabled:cursor-not-allowed disabled:border-gray-200 disabled:bg-gray-100 disabled:text-gray-400"
    >
      {!available ? "No disponible" : busy ? "Descargando..." : "Descargar"}
    </button>
  </div>
);

/** Plantilla y manual que el ADMIN publica en Recursos de carga. */
const UploadResources = () => {
  const [resources, setResources] = useState({ template: null, manual: null });
  const [downloading, setDownloading] = useState(null);

  useEffect(() => {
    uploadResourceService
      .getResources()
      .then(setResources)
      .catch((error) => console.error("No se pudieron obtener los recursos de carga:", error));
  }, []);

  const { template, manual } = resources;

  const handleDownload = async (kind, resource, fetchFile, mimeType, errorMessage) => {
    setDownloading(kind);
    try {
      downloadBlob(await fetchFile(), resource.fileName, mimeType);
    } catch (error) {
      showError(await readApiErrorMessage(error, errorMessage));
    } finally {
      setDownloading(null);
    }
  };

  return (
    <div className="mb-8">
      <h3 className="text-sm font-bold text-gray-700 mb-3 uppercase tracking-wider flex items-center">
        <HiOutlineFolderOpen className="w-5 h-5 mr-2 text-red-600" />
        Recursos
      </h3>
      <div className="space-y-2">
        <ResourceItem
          icon={<HiOutlineDownload className="w-5 h-5 text-red-600" />}
          title="Plantilla de carga"
          description="Excel con las columnas y formatos que acepta la carga."
          onDownload={() =>
            handleDownload(
              "template",
              template,
              uploadResourceService.downloadTemplate,
              XLSX_MIME_TYPE,
              "No se pudo descargar la plantilla. Intente nuevamente.",
            )
          }
          available={Boolean(template)}
          busy={downloading === "template"}
        />
        <ResourceItem
          icon={<HiOutlineBookOpen className="w-5 h-5 text-red-600" />}
          title="Manual de uso de la plantilla"
          description="PDF con cómo llenar cada columna y qué se valida."
          onDownload={() =>
            handleDownload(
              "manual",
              manual,
              uploadResourceService.downloadManual,
              PDF_MIME_TYPE,
              "No se pudo descargar el manual. Intente nuevamente.",
            )
          }
          available={Boolean(manual)}
          busy={downloading === "manual"}
        />
      </div>
    </div>
  );
};

export default UploadResources;
