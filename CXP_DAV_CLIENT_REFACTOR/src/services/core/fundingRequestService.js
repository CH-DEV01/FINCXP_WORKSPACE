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
 * token y el proveedor, del convenio. Se envían la fecha de desembolso y el
 * total que mostró la simulación aceptada; si al registrar cambiaron (por
 * ejemplo, se cruzó la hora de corte) el backend responde 409.
 */
const submitFunding = async ({
  masterAgreementId,
  documentIds,
  termVersionId,
  expectedDisbursementDate,
  expectedAmountToBeDisbursed,
}) => {
  const response = await api.post("/v1/funding-requests/submit", {
    masterAgreementId,
    documentIds,
    termVersionId,
    expectedDisbursementDate,
    expectedAmountToBeDisbursed,
  });
  return response.data?.data;
};

export const fundingRequestService = {
  calculateCost,
  submitFunding,
};
