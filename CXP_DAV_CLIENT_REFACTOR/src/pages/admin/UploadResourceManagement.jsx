import React, { useCallback, useEffect, useRef, useState } from "react";
import { FaFolderOpen } from "react-icons/fa";
import Swal from "sweetalert2";

import HeaderCard from "../../components/HeaderCard";
import { PDF_MIME_TYPE, uploadResourceService, XLSX_MIME_TYPE } from "../../services/core/uploadResourceService";
import { downloadBlob, formatDateTime } from "../../utils/format";
import { apiErrorMessage, readApiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors";
import { formatFileSize, MANUAL_RULES, resourceFileError, TEMPLATE_RULES } from "../../utils/uploadResources";

const cardClass = "bg-white rounded-xl shadow-lg border border-gray-200 p-5 font-montserrat";
const primaryButtonClass =
  "py-2 px-4 rounded-lg text-xs font-medium bg-red-600 text-white hover:bg-red-700 shadow-md cursor-pointer transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed disabled:shadow-none";
const secondaryButtonClass =
  "py-2 px-4 rounded-lg text-xs font-medium bg-white text-gray-700 border border-gray-300 hover:bg-gray-100 cursor-pointer transition-colors disabled:opacity-50 disabled:cursor-not-allowed";

/**
 * Tarjeta para publicar un recurso. `noun` va en minúscula y con artículo ("la plantilla"),
 * para armar los textos de los botones y mensajes.
 */
const ResourceCard = ({ id, title, description, noun, rules, resource, mimeType, download, replace, onPublished }) => {
  const fileInputRef = useRef(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const shortNoun = noun.split(" ").slice(1).join(" ");

  const clearSelectedFile = () => {
    setSelectedFile(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    if (!file) return;
    const error = resourceFileError(file, rules);
    if (error) {
      showError(error, "Archivo no válido");
      clearSelectedFile();
      return;
    }
    setSelectedFile(file);
  };

  const handleDownload = async () => {
    try {
      downloadBlob(await download(), resource.fileName, mimeType);
    } catch (error) {
      showError(await readApiErrorMessage(error, `No se pudo descargar ${noun}.`));
    }
  };

  const handlePublish = async () => {
    if (resource) {
      const result = await Swal.fire({
        title: `¿Reemplazar ${noun}?`,
        text: `"${resource.fileName}" dejará de estar disponible y se publicará "${selectedFile.name}".`,
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#dc2626",
        cancelButtonColor: "#4b5563",
        confirmButtonText: "Sí, reemplazar",
        cancelButtonText: "Cancelar",
        reverseButtons: true,
      });
      if (!result.isConfirmed) return;
    }

    showLoading(`Validando y publicando ${noun}.`);
    try {
      await replace(selectedFile);
      showSuccess(`${rules.label} se publicó correctamente.`);
      clearSelectedFile();
      onPublished();
    } catch (error) {
      showError(apiErrorMessage(error, `No se pudo publicar ${noun}.`), "Archivo rechazado");
    }
  };

  return (
    <section className={cardClass}>
      <h2 className="text-xs uppercase font-semibold text-gray-600 mb-1">{title}</h2>
      <p className="text-xs text-gray-500 mb-4">{description}</p>

      <div className="rounded-lg bg-gray-50 border border-gray-200 px-4 py-3 mb-4 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
        {resource ? (
          <div>
            <p className="text-sm font-semibold text-gray-800">
              {resource.fileName}{" "}
              <span className="text-xs font-normal text-gray-500">({formatFileSize(resource.fileSize)})</span>
            </p>
            <p className="text-xs text-gray-500">
              Actualizado el {formatDateTime(resource.updatedAt)}
              {resource.updatedBy && ` por ${resource.updatedBy}`}
            </p>
          </div>
        ) : (
          <p className="text-sm text-gray-500">Sin publicar. En la carga se muestra como &quot;No disponible&quot;.</p>
        )}
        {resource && (
          <button type="button" onClick={handleDownload} className={secondaryButtonClass}>
            Descargar vigente
          </button>
        )}
      </div>

      <div className="flex flex-col md:flex-row md:items-center gap-3">
        <input
          ref={fileInputRef}
          id={id}
          type="file"
          accept={rules.extension}
          onChange={handleFileChange}
          className="sr-only"
        />
        <label htmlFor={id} className={`${secondaryButtonClass} text-center`}>
          Seleccionar archivo
        </label>
        <span className="text-xs text-gray-600 truncate flex-1">
          {selectedFile ? `${selectedFile.name} (${formatFileSize(selectedFile.size)})` : "Ningún archivo seleccionado"}
        </span>
        <button type="button" onClick={handlePublish} disabled={!selectedFile} className={primaryButtonClass}>
          Publicar {shortNoun}
        </button>
      </div>
    </section>
  );
};

/** Plantilla y manual que se ofrecen en las pantallas de carga del administrador y del pagador. */
const UploadResourceManagement = () => {
  const [resources, setResources] = useState({ template: null, manual: null });

  const fetchResources = useCallback(async () => {
    try {
      setResources(await uploadResourceService.getResources());
    } catch (error) {
      showError(apiErrorMessage(error, "No se pudieron cargar los recursos de carga."));
    }
  }, []);

  useEffect(() => {
    fetchResources();
  }, [fetchResources]);

  return (
    <div className="w-full min-h-screen">
      <HeaderCard
        title="Recursos de carga"
        description="Plantilla y manual que ven el administrador y el pagador en la carga de documentos"
        icon={<FaFolderOpen />}
      />

      <div className="border-b border-gray-200 mb-6"></div>

      <div className="space-y-6">
        <ResourceCard
          id="template-file"
          title="Plantilla de carga"
          description={`Archivo .xlsx sin macros, de hasta ${TEMPLATE_RULES.maxSizeMb} MB. La primera fila de la primera hoja debe tener exactamente las columnas que acepta la carga; si no coinciden, la plantilla se rechaza.`}
          noun="la plantilla"
          rules={TEMPLATE_RULES}
          resource={resources.template}
          mimeType={XLSX_MIME_TYPE}
          download={uploadResourceService.downloadTemplate}
          replace={uploadResourceService.replaceTemplate}
          onPublished={fetchResources}
        />
        <ResourceCard
          id="manual-file"
          title="Manual de uso de la plantilla"
          description={`Archivo .pdf de hasta ${MANUAL_RULES.maxSizeMb} MB, sin contraseña, JavaScript ni archivos incrustados. Si la plantilla cambia, actualice también el manual.`}
          noun="el manual"
          rules={MANUAL_RULES}
          resource={resources.manual}
          mimeType={PDF_MIME_TYPE}
          download={uploadResourceService.downloadManual}
          replace={uploadResourceService.replaceManual}
          onPublished={fetchResources}
        />
      </div>
    </div>
  );
};

export default UploadResourceManagement;
