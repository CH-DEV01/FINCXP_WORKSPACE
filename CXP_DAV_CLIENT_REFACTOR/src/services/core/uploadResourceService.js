import api from "../api";

const BASE = "/v1/upload-resources";

export const XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
export const PDF_MIME_TYPE = "application/pdf";

const download = async (path) => {
  const response = await api.get(`${BASE}/${path}`, { responseType: "blob" });
  return response.data;
};

const replace = async (path, file) => {
  const formData = new FormData();
  formData.append("file", file);
  const response = await api.put(`${BASE}/${path}`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return response.data?.data;
};

/** Plantilla y manual de las pantallas de carga; template o manual son null si el ADMIN no los ha publicado. */
export const uploadResourceService = {
  getResources: async () => {
    const response = await api.get(BASE);
    return response.data?.data ?? { template: null, manual: null };
  },

  downloadTemplate: () => download("template"),

  downloadManual: () => download("manual"),

  replaceTemplate: (file) => replace("template", file),

  replaceManual: (file) => replace("manual", file),
};
