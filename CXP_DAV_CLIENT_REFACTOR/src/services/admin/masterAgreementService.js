import api from "../api";

/** Página de convenios marco (page en base 0). */
/** Con payerId, solo los convenios de ese pagador; la búsqueda es entonces por nombre del proveedor. */
const getMasterAgreements = async ({ page = 0, size = 10, search = "", payerId } = {}) => {
  const params = { page, size };
  if (search) params.search = search;
  if (payerId) params.payerId = payerId;
  const response = await api.get("/v1/master-agreements", { params });
  return response.data?.data;
};

/** Convenios marco (lista) asociados a un proveedor. */
const getMasterAgreementsBySupplier = async (supplierId) => {
  const response = await api.get(`/v1/master-agreements/supplier/${supplierId}`);
  return response.data?.data;
};

/** @param {Object} requestData - MasterAgreementDTORequest. */
const createMasterAgreement = async (requestData) => {
  const response = await api.post("/v1/master-agreements", requestData);
  return response.data?.data;
};

/** @param {Object} requestData - MasterAgreementDTORequest. */
const updateMasterAgreement = async (id, requestData) => {
  const response = await api.put(`/v1/master-agreements/${id}`, requestData);
  return response.data?.data;
};

/** Próxima fecha de desembolso según la política del convenio: { nextDisbursementDate, cutoffTime, ... }. */
const getNextDisbursementDate = async (masterAgreementId) => {
  const response = await api.get(`/v1/master-agreements/${masterAgreementId}/next-disbursement-date`);
  return response.data?.data;
};

export const masterAgreementService = {
  getMasterAgreements,
  getMasterAgreementsBySupplier,
  getNextDisbursementDate,
  createMasterAgreement,
  updateMasterAgreement,
};
