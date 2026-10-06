import api from "../api";

const getRoles = async () => {
  const response = await api.get("/v1/roles");
  return response.data?.data ?? [];
};

export const roleService = {
  getRoles,
};
