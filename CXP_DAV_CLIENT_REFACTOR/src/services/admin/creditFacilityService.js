import api from "../api";

const getPayerCreditLineDetails = async (payerId) => {
  const response = await api.get(`/v1/credit-facilities/payer/${payerId}`);
  return response.data?.data;
};

const restoreCreditFacility = async ({ creditFacilityId, amount, reference, type, disbursementBatchId }) => {
  const response = await api.post("/v1/credit-facilities/restore", {
    creditFacilityId,
    amount,
    reference,
    type,
    disbursementBatchId,
  });
  return response.data?.data;
};

/** Lotes desembolsados del pagador que aún no se han abonado al cupo. */
const getRestorableBatches = async (creditFacilityId) => {
  const response = await api.get(`/v1/credit-facilities/${creditFacilityId}/restorable-batches`);
  return response.data?.data ?? [];
};

/** Historial paginado (page en base 0): content, totalPages, totalElements. */
const getHistory = async (creditFacilityId, { page = 0, size = 10 } = {}) => {
  const response = await api.get(`/v1/credit-facilities/history/${creditFacilityId}`, {
    params: { page, size },
  });
  return response.data?.data;
};

export const creditFacilityService = {
  getPayerCreditLineDetails,
  restoreCreditFacility,
  getRestorableBatches,
  getHistory,
};
