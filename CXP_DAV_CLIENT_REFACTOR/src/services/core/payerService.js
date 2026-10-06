import api from "../api";

export const OWN_PAYER = "me";

/**
 * Datos de la entidad del pagador y su línea de crédito (creditLine es null si no tiene).
 * Con OWN_PAYER consulta el pagador autenticado; con un id, el operador bancario consulta ese pagador.
 */
const getSummary = async (payerId = OWN_PAYER) => {
  const response = await api.get(`/v1/payers/${payerId}/summary`);
  return response.data?.data;
};

export const payerService = {
  getSummary,
};
