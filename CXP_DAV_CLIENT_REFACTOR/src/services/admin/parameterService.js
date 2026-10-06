import api from "../api";

/** Página de parámetros del sistema. */
const getParameters = async () => {
  const response = await api.get("/v1/parameters", { params: { size: 1000 } });
  return response.data?.data;
};

const updateParameter = async (id, payload) => {
  const response = await api.put(`/v1/parameters/${id}`, payload);
  return response.data?.data;
};

export const parameterService = {
  getParameters,
  updateParameter,
};
