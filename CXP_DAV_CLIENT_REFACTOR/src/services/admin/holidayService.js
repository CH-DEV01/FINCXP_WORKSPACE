import api from "../api";

const BASE = "/v1/holidays";

export const holidayService = {
  getHolidays: async () => {
    const response = await api.get(BASE);
    return response.data?.data ?? [];
  },

  createHoliday: async (data) => {
    const response = await api.post(BASE, data);
    return response.data?.data;
  },

  updateHoliday: async (id, data) => {
    const response = await api.put(`${BASE}/${id}`, data);
    return response.data?.data;
  },

  deleteHoliday: async (id) => {
    await api.delete(`${BASE}/${id}`);
  },
};
