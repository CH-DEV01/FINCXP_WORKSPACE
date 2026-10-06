import api from "../api";

/** Simula el cálculo financiero de una solicitud de fondeo sin persistir (CalculateDTOResponse). */
const calculateCost = async ({ masterAgreementId, documentIds }) => {
  const response = await api.post("/v1/funding-requests/calculate", {
    masterAgreementId,
    documentIds,
  });
  return response.data?.data;
};

/**
 * Persiste la solicitud de desembolso / fondeo. El solicitante se toma del
 * token y el proveedor, del convenio.
 */
const submitFunding = async ({ masterAgreementId, documentIds, termVersionId }) => {
  const response = await api.post("/v1/funding-requests/submit", {
    masterAgreementId,
    documentIds,
    termVersionId,
  });
  return response.data?.data;
};

export const fundingRequestService = {
  calculateCost,
  submitFunding,
};
