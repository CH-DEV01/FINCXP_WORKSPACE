import api from "../api";

const BASE = "/v1/term-versions";

const ACTIVE_PATH_BY_TYPE = {
  SUPPLIER_TERM_TYPE: `${BASE}/active/supplier`,
  PAYER_TERM_TYPE: `${BASE}/active/payer`,
};

export const termService = {
  getActive: async (termTypeCode) => {
    const path = ACTIVE_PATH_BY_TYPE[termTypeCode];
    if (!path) {
      throw new Error(`Tipo de término no soportado: ${termTypeCode}`);
    }
    const response = await api.get(path);
    return response.data?.data;
  },

  getTypes: async () => {
    const response = await api.get(`${BASE}/types`);
    return response.data?.data ?? [];
  },

  getVersions: async (termTypeCode) => {
    const response = await api.get(BASE, { params: { termTypeCode } });
    return response.data?.data ?? [];
  },

  createDraft: async (data) => {
    const response = await api.post(BASE, data);
    return response.data?.data;
  },

  updateDraft: async (id, data) => {
    const response = await api.put(`${BASE}/${id}`, data);
    return response.data?.data;
  },

  deleteDraft: async (id) => {
    await api.delete(`${BASE}/${id}`);
  },

  publish: async (id) => {
    const response = await api.post(`${BASE}/${id}/publish`);
    return response.data?.data;
  },
};
