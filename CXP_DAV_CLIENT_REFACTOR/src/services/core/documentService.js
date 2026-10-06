import api from "../api";

/**
 * Documentos APPROVED de un convenio marco que aún son financiables
 * (su fecha de vencimiento no cae dentro de la ventana de gracia del backend).
 */
const getFinanceableByMasterAgreement = async (masterAgreementId) => {
  const response = await api.get(`/v1/documents/master-agreement/${masterAgreementId}/financeable`);
  return response.data?.data ?? [];
};

const withoutEmpty = (params) =>
  Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ""),
  );

/**
 * Página de la bitácora de documentos de un proveedor.
 * @param {Object} filters - { payerId, status, search, page (base 0), size }.
 */
const getHistoryBySupplier = async (supplierId, filters = {}) => {
  const response = await api.get(`/v1/documents/supplier/${supplierId}/history`, {
    params: withoutEmpty(filters),
  });
  return response.data?.data;
};

/**
 * Página de la bitácora de documentos cargados por un pagador.
 * @param {Object} filters - { supplierId, status, search, page (base 0), size }.
 */
const getHistoryByPayer = async (payerId, filters = {}) => {
  const response = await api.get(`/v1/documents/payer/${payerId}/history`, {
    params: withoutEmpty(filters),
  });
  return response.data?.data;
};

/** Cantidad y monto de los documentos del pagador por proveedor y estado. */
const getHistorySummaryByPayer = async (payerId) => {
  const response = await api.get(`/v1/documents/payer/${payerId}/history/summary`);
  return response.data?.data ?? [];
};

/** El pagador pasa a Inactivo un documento Cargado, sea financiable o no. */
const inactivateDocument = async (documentId) => {
  const response = await api.patch(`/v1/documents/${documentId}/inactivate`);
  return response.data?.data;
};

export const documentService = {
  getFinanceableByMasterAgreement,
  getHistoryBySupplier,
  getHistoryByPayer,
  getHistorySummaryByPayer,
  inactivateDocument,
};
