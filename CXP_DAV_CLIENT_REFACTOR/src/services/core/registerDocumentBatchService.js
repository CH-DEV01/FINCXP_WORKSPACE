import api from "../api";

/**
 * Envía el archivo Excel al backend y retorna el Blob del PDF (comprobante).
 * @param {File} file - Archivo Excel seleccionado
 * @param {string} payerId - UUID del pagador
 * @param {string} [termVersionId] - Versión de términos del pagador que aceptó el usuario
 */
const uploadBatch = async (file, payerId, termVersionId) => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("payerId", payerId);
  if (termVersionId) {
    formData.append("termVersionId", termVersionId);
  }

  const response = await api.post("/v1/batches/upload", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    responseType: "blob",
  });
  return response.data;
};

/** Extensiones, tamaño máximo (MB) y filas máximas configurados en los parámetros del sistema. */
const getUploadSettings = async () => {
  const response = await api.get("/v1/batches/upload-settings");
  return response.data?.data;
};

export const registerDocumentBatchService = {
  uploadBatch,
  getUploadSettings,
};
