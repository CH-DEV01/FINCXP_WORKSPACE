import api from "../api";

const BASE_URL = "/v1/dispersion-requests";

/**
 * Pagadores para la terminal de dispersiones:
 * [{ id, name, accountNumber, pendingGroups }]
 */
const getPayers = async () => {
  const response = await api.get(`${BASE_URL}/payers`);
  return response.data?.data ?? [];
};

/**
 * Solicitudes de dispersión del pagador: vencimientos pendientes de lote y lotes generados.
 * [{ batchId, batchNumber, dueDate, dispersionDate, documentCount, supplierCount, totalAmount,
 *    status: 'INGRESADO' | 'EN_PROCESO' | 'DISPERSADO', batchCreatedAt }]
 */
const getPayerRequests = async (payerId) => {
  const response = await api.get(`${BASE_URL}/payers/${payerId}/requests`);
  return response.data?.data ?? [];
};

/**
 * Documentos pendientes de dispersar de un vencimiento:
 * [{ id, dteNumber, supplierId, supplierName, accountNumber, dueDate, nominalAmount, status }]
 */
const getPendingDocuments = async (payerId, dueDate) => {
  const response = await api.get(`${BASE_URL}/payers/${payerId}/requests/documents`, {
    params: { dueDate },
  });
  return response.data?.data ?? [];
};

/**
 * Genera el lote de un vencimiento y devuelve el Blob de la solicitud de dispersión.
 * payload: { payerId, dueDate }; la dispersión se realiza en la misma fecha de vencimiento.
 */
const generateBatch = async (payload) => {
  const response = await api.post(`${BASE_URL}/generate-batch`, payload, {
    responseType: "blob",
  });
  return response.data;
};

/**
 * Bitácora de lotes de dispersión paginada (page en base 0); payerId opcional:
 * { content: [{ id, batchNumber, payerId, payerName, payerAccountNumber, dueDate, dispersionDate,
 *    documentCount, totalAmount, status: 'CREATED' | 'SETTLED', createdByName, confirmedByName,
 *    createdAt, confirmedAt }],
 *   number, size, totalPages, totalElements, totalAmount }
 */
const getBatchHistory = async ({ payerId, page = 0, size = 10 } = {}) => {
  const response = await api.get(`${BASE_URL}/batches`, {
    params: payerId ? { payerId, page, size } : { page, size },
  });
  return response.data?.data;
};

/** Detalle de un lote: { batch, signerName, documents: [mismo formato que getPendingDocuments] } */
const getBatchDetails = async (batchId) => {
  const response = await api.get(`${BASE_URL}/batches/${batchId}`);
  return response.data?.data;
};

/** Confirma la dispersión del lote: todos sus documentos pasan a dispersados. */
const confirmBatch = async (batchId) => {
  const response = await api.post(`${BASE_URL}/batches/${batchId}/confirm`);
  return response.data?.data;
};

/** Re-genera la solicitud de dispersión de un lote existente y devuelve su Blob. */
const redownloadBatch = async (batchId) => {
  const response = await api.get(`${BASE_URL}/batches/${batchId}/download`, {
    responseType: "blob",
  });
  return response.data;
};

export const dispersionService = {
  getPayers,
  getPayerRequests,
  getPendingDocuments,
  generateBatch,
  getBatchHistory,
  getBatchDetails,
  confirmBatch,
  redownloadBatch,
};
