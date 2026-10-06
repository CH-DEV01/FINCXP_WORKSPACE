import api from "../api";

/** Entidades del tipo que exige el rol (page en base 0); search compara nombre y NIT. */
const getEntitiesForRole = async (roleId, { search, page = 0, size = 10 } = {}) => {
  const response = await api.get(`/v1/entities/for-role/${roleId}`, {
    params: search ? { search, page, size } : { page, size },
  });
  return response.data?.data;
};

/**
 * Actualiza solo los atributos editables del pagador: tasas, base de cálculo, estado y umbral.
 * Las tasas y el umbral deben enviarse como fracción decimal (0.155 = 15.5 %).
 */
const updatePayer = async (id, payload) => {
  const response = await api.put(`/v1/entities/payers/${id}`, payload);
  return response.data?.data;
};

/** Actualiza solo los atributos editables del proveedor: cuenta bancaria principal y estado. */
const updateSupplier = async (id, payload) => {
  const response = await api.put(`/v1/entities/suppliers/${id}`, payload);
  return response.data?.data;
};

/** Resumen paginado de proveedores (page en base 0); search compara nombre y NIT. */
const getSupplierSummaries = async ({ search, page = 0, size = 10 } = {}) => {
  const response = await api.get("/v1/entities/suppliers/summary", {
    params: search ? { search, page, size } : { page, size },
  });
  return response.data?.data;
};

/** Página del catálogo de pagadores. */
const getPayers = async (page = 0, size = 100, sortBy = "name", sortDir = "ASC") => {
  const response = await api.get("/v1/entities/payers", {
    params: { page, size, sortBy, sortDir },
  });
  return response.data?.data;
};

/** Página del catálogo de proveedores. */
const getSuppliers = async (page = 0, size = 100, sortBy = "name", sortDir = "ASC") => {
  const response = await api.get("/v1/entities/suppliers", {
    params: { page, size, sortBy, sortDir },
  });
  return response.data?.data;
};

/**
 * Registra un pagador junto con su cupo de crédito y su tarifario en una sola operación.
 * Las tasas y el umbral deben enviarse como fracción decimal (0.155 = 15.5 %).
 */
const registerPayer = async (payload) => {
  const response = await api.post("/v1/entities/payers", payload);
  return response.data?.data;
};

/** Resumen paginado de pagadores (page en base 0); search compara nombre y NIT. */
const getPayerSummaries = async ({ search, page = 0, size = 10 } = {}) => {
  const response = await api.get("/v1/entities/payers/summary", {
    params: search ? { search, page, size } : { page, size },
  });
  return response.data?.data;
};

export const entityService = {
  registerPayer,
  getPayerSummaries,
  getEntitiesForRole,
  getSuppliers,
  updatePayer,
  updateSupplier,
  getSupplierSummaries,
  getPayers,
};
