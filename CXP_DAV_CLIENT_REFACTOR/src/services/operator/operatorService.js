import api from "../api";

const BASE_URL = "/v1/disbursement-requests";

/**
 * Resumen de pagadores para la terminal de desembolsos:
 * [{ id, name, creditLineNumber, creditLineAvailableAmount, availableRequests }]
 */
const getPayersResume = async () => {
  const response = await api.get(`${BASE_URL}/payers`);
  return response.data?.data ?? [];
};

/**
 * Solicitudes del pagador (combinaciones vencimiento/solicitud/desembolso):
 * [{ batchId, batchNumber, dueDate, requestDate, disbursementDate, documentCount, supplierCount, totalAmount,
 *    status: 'INGRESADO' | 'EN_PROCESO' | 'DESEMBOLSADO', batchCreatedAt }]
 */
const getPayerRequests = async (payerId) => {
  const response = await api.get(`${BASE_URL}/payers/${payerId}/requests`);
  return response.data?.data ?? [];
};

/**
 * Detalle de una solicitud agrupado por proveedor.
 * params: { batchId } o { dueDate, requestDate, disbursementDate }
 * [{ supplierId, supplierName, accountNumber, documentCount, totalAmount, amountToCredit, dueDate }]
 */
const getRequestSuppliers = async (payerId, params) => {
  const response = await api.get(`${BASE_URL}/payers/${payerId}/requests/suppliers`, { params });
  return response.data?.data ?? [];
};

/**
 * Genera el lote de una combinación y devuelve el Blob de su PDF.
 * payload: { payerId, originalFileName, groups: [{ dueDate, requestDate, disbursementDate }] } (un solo grupo)
 */
const generateBatch = async (payload) => {
  const response = await api.post(`${BASE_URL}/generate-batch`, payload, {
    responseType: "blob",
  });
  return response.data;
};

/**
 * Bitácora de lotes paginada (page en base 0); payerId opcional para filtrar:
 * { content: [{ id, batchNumber, outputFileName, documentCount, totalAmountToDisburse, totalCommission,
 *    totalInterest, status, createdByName, confirmedByName, payerId, payerName, dueDate, requestDate,
 *    disbursementDate, createdAt, updatedAt }],
 *   number, size, totalPages, totalElements, totalAmountToDisburse }
 */
const getBatchHistory = async ({ payerId, page = 0, size = 10 } = {}) => {
  const response = await api.get(`${BASE_URL}/batches`, {
    params: payerId ? { payerId, page, size } : { page, size },
  });
  return response.data?.data;
};

/**
 * Detalle de un lote con sus documentos:
 * { id, batchNumber, status, payerName, documents: [{ id, documentNumber, supplierName, amountToFinance, amountToBeDisbursed, ... }] }
 */
const getBatchDetails = async (batchId) => {
  const response = await api.get(`${BASE_URL}/batches/${batchId}`);
  return response.data?.data;
};

/** Confirma el lote completo: todos sus documentos pasan a desembolsados. */
const confirmBatch = async (batchId) => {
  const response = await api.post(`${BASE_URL}/batches/${batchId}/confirm`);
  return response.data?.data;
};

/** Re-genera el PDF del reporte de un lote existente y devuelve su Blob. */
const redownloadBatch = async (batchId) => {
  const response = await api.get(`${BASE_URL}/batches/${batchId}/download`, {
    responseType: "blob",
  });
  return response.data;
};

export const operatorService = {
  getPayersResume,
  getPayerRequests,
  getRequestSuppliers,
  generateBatch,
  getBatchHistory,
  getBatchDetails,
  confirmBatch,
  redownloadBatch,
};
