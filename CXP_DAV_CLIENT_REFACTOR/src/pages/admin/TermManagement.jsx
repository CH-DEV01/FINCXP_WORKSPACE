import React, { useCallback, useEffect, useMemo, useState } from "react";
import Swal from "sweetalert2";
import { FaFileContract, FaPlus, FaEye, FaEdit, FaTrash, FaUpload, FaArrowLeft } from "react-icons/fa";
import HeaderCard from "../../components/HeaderCard";
import TermsMarkdown from "../../components/terms/TermsMarkdown.jsx";
import { termService } from "../../services/terms/termService.js";
import escapeHtml from "../../utils/escapeHtml.js";
import BaseStatusBadge from "../../components/StatusBadge.jsx";
import inputClass from "../../components/form/inputClass.js";
import { TERM_BADGE_CLASS, TERM_VERSION_STATUS } from "../../constants/status.js";
import { formatDate as formatIsoDate } from "../../utils/format.js";
import { apiErrorMessage, showError } from "../../utils/errors.js";

const EMPTY_FORM = {
  versionNumber: "",
  title: "",
  content: "",
  acceptanceText: "",
  documentUrl: "",
};

const formatDate = (isoDate) => formatIsoDate(isoDate, "—");

const compareVersions = (a, b) => {
  const pa = a.split(".").map(Number);
  const pb = b.split(".").map(Number);
  for (let i = 0; i < Math.max(pa.length, pb.length); i += 1) {
    const diff = (pa[i] ?? 0) - (pb[i] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
};

const suggestNextVersion = (versions) => {
  const numbers = versions.map((v) => v.versionNumber).filter((n) => /^\d+(\.\d+)*$/.test(n));
  if (numbers.length === 0) return "1.0";
  const highest = [...numbers].sort(compareVersions).at(-1).split(".").map(Number);
  if (highest.length === 1) return `${highest[0]}.1`;
  highest[highest.length - 1] += 1;
  return highest.join(".");
};

const StatusBadge = ({ status }) => (
  <BaseStatusBadge status={status} statusMap={TERM_VERSION_STATUS} className={TERM_BADGE_CLASS} />
);

const TermManagement = () => {
  const [termTypes, setTermTypes] = useState([]);
  const [selectedType, setSelectedType] = useState("");
  const [versions, setVersions] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  // mode: "list" | "edit" | "view"
  const [mode, setMode] = useState("list");
  const [currentVersion, setCurrentVersion] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [initialForm, setInitialForm] = useState(EMPTY_FORM);
  const [isSaving, setIsSaving] = useState(false);

  const activeVersion = useMemo(() => versions.find((v) => v.status === "ACTIVE"), [versions]);
  const isDirty = JSON.stringify(form) !== JSON.stringify(initialForm);

  const loadVersions = useCallback(async (typeCode) => {
    if (!typeCode) return;
    setIsLoading(true);
    try {
      setVersions(await termService.getVersions(typeCode));
    } catch (error) {
      console.error("Error al cargar las versiones de términos:", error);
      setVersions([]);
      showError(apiErrorMessage(error, "No se pudieron cargar las versiones."));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    termService
      .getTypes()
      .then((types) => {
        setTermTypes(types);
        if (types.length > 0) setSelectedType(types[0].uniqueCode);
      })
      .catch((error) => {
        console.error("Error al cargar los tipos de término:", error);
        showError(apiErrorMessage(error, "No se pudieron cargar los tipos de término."));
      });
  }, []);

  useEffect(() => {
    loadVersions(selectedType);
  }, [selectedType, loadVersions]);

  const confirmDiscardChanges = async () => {
    if (mode !== "edit" || !isDirty) return true;
    const result = await Swal.fire({
      icon: "warning",
      title: "Cambios sin guardar",
      text: "Si sale del editor perderá los cambios no guardados.",
      showCancelButton: true,
      confirmButtonText: "Descartar cambios",
      cancelButtonText: "Seguir editando",
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#4b5563",
      reverseButtons: true,
    });
    return result.isConfirmed;
  };

  const backToList = async () => {
    if (!(await confirmDiscardChanges())) return;
    setMode("list");
    setCurrentVersion(null);
  };

  const handleSelectType = async (typeCode) => {
    if (typeCode === selectedType) return;
    if (!(await confirmDiscardChanges())) return;
    setMode("list");
    setCurrentVersion(null);
    setSelectedType(typeCode);
  };

  const openEditor = (version, values) => {
    setCurrentVersion(version);
    setForm(values);
    setInitialForm(values);
    setMode("edit");
  };

  const handleCreate = () => {
    if (versions.some((v) => v.status === "DRAFT")) {
      Swal.fire({
        icon: "info",
        title: "Ya existe un borrador",
        text: "Edite o elimine el borrador existente antes de crear otra versión.",
      });
      return;
    }
    openEditor(null, {
      versionNumber: suggestNextVersion(versions),
      title: activeVersion?.title ?? "",
      content: activeVersion?.content ?? "",
      acceptanceText: activeVersion?.acceptanceText ?? "",
      documentUrl: activeVersion?.documentUrl ?? "",
    });
  };

  const handleEdit = (version) => {
    openEditor(version, {
      versionNumber: version.versionNumber,
      title: version.title,
      content: version.content,
      acceptanceText: version.acceptanceText,
      documentUrl: version.documentUrl ?? "",
    });
  };

  const handleView = (version) => {
    setCurrentVersion(version);
    setMode("view");
  };

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const saveDraft = async () => {
    const payload = { ...form, termTypeCode: selectedType };
    const saved = currentVersion
      ? await termService.updateDraft(currentVersion.id, payload)
      : await termService.createDraft(payload);
    setCurrentVersion(saved);
    setInitialForm(form);
    return saved;
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      await saveDraft();
      await loadVersions(selectedType);
      Swal.fire({ icon: "success", title: "Borrador guardado", timer: 1500, showConfirmButton: false });
    } catch (error) {
      console.error("Error al guardar el borrador:", error);
      showError(apiErrorMessage(error, "Intente nuevamente."), "No se pudo guardar");
    } finally {
      setIsSaving(false);
    }
  };

  const handlePublish = async (version) => {
    const fromEditor = mode === "edit";
    const versionNumber = fromEditor ? form.versionNumber : version.versionNumber;

    const result = await Swal.fire({
      icon: "warning",
      title: `¿Publicar la versión ${escapeHtml(versionNumber)}?`,
      html: activeVersion
        ? `La versión vigente <b>${escapeHtml(activeVersion.versionNumber)}</b> quedará como histórica y a partir de ahora los usuarios aceptarán el nuevo texto.<br/><br/>Una versión publicada no se puede modificar ni eliminar.`
        : "A partir de ahora los usuarios aceptarán este texto.<br/><br/>Una versión publicada no se puede modificar ni eliminar.",
      showCancelButton: true,
      confirmButtonText: "Sí, publicar",
      cancelButtonText: "Cancelar",
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#4b5563",
      reverseButtons: true,
    });
    if (!result.isConfirmed) return;

    setIsSaving(true);
    try {
      const draft =
        fromEditor && (isDirty || !currentVersion) ? await saveDraft() : fromEditor ? currentVersion : version;
      await termService.publish(draft.id);
      await loadVersions(selectedType);
      setMode("list");
      setCurrentVersion(null);
      Swal.fire({ icon: "success", title: "Versión publicada", timer: 1800, showConfirmButton: false });
    } catch (error) {
      console.error("Error al publicar la versión:", error);
      showError(apiErrorMessage(error, "Intente nuevamente."), "No se pudo publicar");
      await loadVersions(selectedType);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (version) => {
    const result = await Swal.fire({
      icon: "warning",
      title: "¿Eliminar el borrador?",
      text: `Se eliminará el borrador de la versión ${version.versionNumber}. Esta operación no se puede deshacer.`,
      showCancelButton: true,
      confirmButtonText: "Sí, eliminar",
      cancelButtonText: "Cancelar",
      confirmButtonColor: "#dc2626",
      cancelButtonColor: "#4b5563",
      reverseButtons: true,
    });
    if (!result.isConfirmed) return;

    try {
      await termService.deleteDraft(version.id);
      await loadVersions(selectedType);
      if (currentVersion?.id === version.id) {
        setMode("list");
        setCurrentVersion(null);
      }
      Swal.fire({ icon: "success", title: "Borrador eliminado", timer: 1500, showConfirmButton: false });
    } catch (error) {
      console.error("Error al eliminar el borrador:", error);
      showError(apiErrorMessage(error, "Intente nuevamente."), "No se pudo eliminar");
    }
  };

  const renderTypeTabs = () => (
    <div className="shrink-0 flex flex-wrap gap-2 mt-4">
      {termTypes.map((type) => (
        <button
          key={type.uniqueCode}
          onClick={() => handleSelectType(type.uniqueCode)}
          className={`font-montserrat shadow-md hover:cursor-pointer py-1 px-3 rounded-full text-xs font-medium transition-all ${
            selectedType === type.uniqueCode ? "bg-red-600 text-white" : "bg-gray-200 text-gray-700 hover:bg-gray-300"
          }`}
        >
          {type.termName}
        </button>
      ))}
    </div>
  );

  const renderList = () => (
    <div className="mt-4 flex-1 min-h-0 flex flex-col gap-4">
      <div className="shrink-0 flex flex-col md:flex-row md:items-center md:justify-between gap-3 bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
        <div className="text-xs font-montserrat text-gray-600">
          {activeVersion ? (
            <>
              <p className="font-semibold text-gray-800">
                Vigente: versión {activeVersion.versionNumber} — {activeVersion.title}
              </p>
              <p className="mt-1">
                Publicada el {formatDate(activeVersion.publicationDate)}
                {activeVersion.publishedByName && ` por ${activeVersion.publishedByName}`} ·{" "}
                {activeVersion.acceptanceCount} aceptaciones
              </p>
            </>
          ) : (
            <p className="font-semibold text-red-700">
              No hay una versión vigente: los usuarios no podrán aceptar términos de este tipo.
            </p>
          )}
        </div>
        <button
          onClick={handleCreate}
          className="font-montserrat flex items-center justify-center bg-red-600 hover:bg-red-700 text-white py-2 px-4 rounded-md text-xs font-medium shadow-lg hover:cursor-pointer"
        >
          <FaPlus className="mr-2" /> Nueva versión
        </button>
      </div>

      <div className="flex-1 min-h-[300px] lg:min-h-0 bg-white border border-gray-200 rounded-lg shadow-sm overflow-auto">
        <table className="min-w-full text-xs font-montserrat">
          <thead className="sticky top-0 z-10 bg-gray-50 text-gray-600 uppercase">
            <tr>
              <th className="px-4 py-3 text-left font-semibold">Versión</th>
              <th className="px-4 py-3 text-left font-semibold">Título</th>
              <th className="px-4 py-3 text-left font-semibold">Estado</th>
              <th className="px-4 py-3 text-left font-semibold">Publicación</th>
              <th className="px-4 py-3 text-left font-semibold">Publicada por</th>
              <th className="px-4 py-3 text-right font-semibold">Aceptaciones</th>
              <th className="px-4 py-3 text-right font-semibold">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 text-gray-700">
            {isLoading && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
                  Cargando versiones...
                </td>
              </tr>
            )}
            {!isLoading && versions.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
                  No hay versiones registradas.
                </td>
              </tr>
            )}
            {!isLoading &&
              versions.map((version) => (
                <tr key={version.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-semibold">{version.versionNumber}</td>
                  <td className="px-4 py-3 max-w-xs truncate" title={version.title}>
                    {version.title}
                  </td>
                  <td className="px-4 py-3">
                    <StatusBadge status={version.status} />
                  </td>
                  <td className="px-4 py-3">{formatDate(version.publicationDate)}</td>
                  <td className="px-4 py-3">{version.publishedByName ?? "—"}</td>
                  <td className="px-4 py-3 text-right">{version.acceptanceCount}</td>
                  <td className="px-4 py-3">
                    <div className="flex justify-end gap-3 text-sm">
                      <button
                        title="Ver"
                        onClick={() => handleView(version)}
                        className="text-gray-500 hover:text-gray-800 hover:cursor-pointer"
                      >
                        <FaEye />
                      </button>
                      {version.status === "DRAFT" && (
                        <>
                          <button
                            title="Editar"
                            onClick={() => handleEdit(version)}
                            className="text-blue-600 hover:text-blue-800 hover:cursor-pointer"
                          >
                            <FaEdit />
                          </button>
                          <button
                            title="Publicar"
                            onClick={() => handlePublish(version)}
                            className="text-green-600 hover:text-green-800 hover:cursor-pointer"
                          >
                            <FaUpload />
                          </button>
                          <button
                            title="Eliminar"
                            onClick={() => handleDelete(version)}
                            className="text-red-600 hover:text-red-800 hover:cursor-pointer"
                          >
                            <FaTrash />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  );

  const renderPreview = (values) => (
    <div className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
      <h2 className="text-base font-bold border-b pb-2 mb-3">{values.title || "Sin título"}</h2>
      <TermsMarkdown content={values.content} />
      {values.acceptanceText && (
        <div className="flex items-start mt-4 pt-3 border-t">
          <input type="checkbox" disabled className="w-4 h-4 mt-0.5 shrink-0" />
          <span className="ml-3 text-sm font-medium text-gray-900">{values.acceptanceText}</span>
        </div>
      )}
    </div>
  );

  const renderFormatHelp = () => (
    <p className="text-[11px] text-gray-500">
      Formato: <code>**negrita**</code>, <code>## Subtítulo</code>, líneas en blanco entre párrafos, listas con{" "}
      <code>- </code> (se muestran sin viñeta; use letras si las necesita) y citas con <code>&gt; </code>.
    </p>
  );

  const renderEditor = () => (
    <div className="mt-4 space-y-4">
      <div className="flex items-center justify-between">
        <button
          onClick={backToList}
          className="flex items-center text-xs text-gray-600 hover:text-gray-900 hover:cursor-pointer"
        >
          <FaArrowLeft className="mr-2" /> Volver a las versiones
        </button>
        <span className="text-xs text-gray-500 font-montserrat">
          {currentVersion ? `Editando borrador ${currentVersion.versionNumber}` : "Nuevo borrador"}
          {isDirty && " · cambios sin guardar"}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
        <div>
          <label className="block text-xs text-gray-600 mb-2">Número de versión</label>
          <input
            name="versionNumber"
            value={form.versionNumber}
            onChange={handleChange}
            className={inputClass()}
            placeholder="1.2"
          />
        </div>
        <div className="md:col-span-3">
          <label className="block text-xs text-gray-600 mb-2">Título</label>
          <input name="title" value={form.title} onChange={handleChange} className={inputClass()} maxLength={255} />
        </div>
        <div className="md:col-span-4">
          <label className="block text-xs text-gray-600 mb-2">Texto de la casilla de aceptación</label>
          <input
            name="acceptanceText"
            value={form.acceptanceText}
            onChange={handleChange}
            className={inputClass()}
            maxLength={500}
          />
        </div>
        <div className="md:col-span-4">
          <label className="block text-xs text-gray-600 mb-2">Enlace al documento firmado (opcional, https)</label>
          <input
            name="documentUrl"
            value={form.documentUrl}
            onChange={handleChange}
            className={inputClass()}
            placeholder="https://..."
            maxLength={500}
          />
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-4">
        <div className="space-y-2">
          <label className="block text-xs text-gray-600">Contenido (Markdown)</label>
          <textarea
            name="content"
            value={form.content}
            onChange={handleChange}
            className={`${inputClass()} font-mono h-[28rem] resize-y`}
          />
          {renderFormatHelp()}
        </div>
        <div className="space-y-2">
          <span className="block text-xs text-gray-600">Vista previa</span>
          <div className="max-h-[32rem] overflow-y-auto">{renderPreview(form)}</div>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row justify-end gap-3 border-t pt-4">
        <button
          onClick={backToList}
          className="py-2 px-4 rounded-lg text-xs font-medium bg-white text-gray-700 border border-gray-300 hover:bg-gray-100 hover:cursor-pointer"
        >
          Cancelar
        </button>
        <button
          onClick={handleSave}
          disabled={isSaving || !isDirty}
          className="py-2 px-4 rounded-lg text-xs font-medium bg-gray-800 text-white hover:bg-gray-900 shadow-md hover:cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Guardar borrador
        </button>
        <button
          onClick={() => handlePublish(currentVersion)}
          disabled={isSaving}
          className="py-2 px-4 rounded-lg text-xs font-medium bg-red-600 text-white hover:bg-red-700 shadow-md hover:cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Guardar y publicar
        </button>
      </div>
    </div>
  );

  const renderView = () => (
    <div className="mt-4 space-y-4">
      <button
        onClick={backToList}
        className="flex items-center text-xs text-gray-600 hover:text-gray-900 hover:cursor-pointer"
      >
        <FaArrowLeft className="mr-2" /> Volver a las versiones
      </button>
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2 text-xs text-gray-600 font-montserrat bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
        <span className="font-semibold text-gray-800">Versión {currentVersion.versionNumber}</span>
        <StatusBadge status={currentVersion.status} />
        <span>Publicación: {formatDate(currentVersion.publicationDate)}</span>
        {currentVersion.publishedByName && <span>Publicada por: {currentVersion.publishedByName}</span>}
        <span>Aceptaciones: {currentVersion.acceptanceCount}</span>
        {currentVersion.documentUrl && (
          <a
            href={currentVersion.documentUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-red-700 underline"
          >
            Ver documento
          </a>
        )}
        {currentVersion.contentHash && (
          <span className="font-mono break-all" title="SHA-256 del título, contenido y texto de aceptación">
            Hash: {currentVersion.contentHash}
          </span>
        )}
      </div>
      {renderPreview(currentVersion)}
    </div>
  );

  return (
    <div
      className={`flex flex-col w-full min-h-0 pb-3 ${mode === "list" ? "lg:h-[calc(100dvh-5.5rem)]" : "min-h-screen"}`}
    >
      <div className="shrink-0">
        <HeaderCard
          title="Términos y condiciones"
          description="Administre las versiones del texto legal que aceptan pagadores y proveedores."
          icon={<FaFileContract />}
        />
      </div>
      <div className="shrink-0 border-b border-gray-200 mb-4"></div>

      {renderTypeTabs()}
      {mode === "list" && renderList()}
      {mode === "edit" && renderEditor()}
      {mode === "view" && currentVersion && renderView()}
    </div>
  );
};

export default TermManagement;
