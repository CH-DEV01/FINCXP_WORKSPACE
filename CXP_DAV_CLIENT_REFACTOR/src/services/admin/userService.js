import api from "../api";

const createUser = async (payload) => {
  const response = await api.post("/v1/users", {
    dui: payload.dui,
    firstName: payload.firstName,
    lastName: payload.lastName,
    email: payload.email,
    entityId: payload.entityId,
    roleId: payload.roleId,
  });
  return response.data?.data;
};

/** Página de usuarios (page en base 0); el backend envía el apellido como "LastName". */
const getUsers = async ({ page = 0, size = 10, search = "" } = {}) => {
  const params = search ? { page, size, search } : { page, size };
  const response = await api.get("/v1/users", { params });
  return response.data?.data;
};

const updateUser = async (id, payload) => {
  const response = await api.put(`/v1/users/${id}`, {
    dui: payload.dui,
    firstName: payload.firstName,
    lastName: payload.lastName,
    email: payload.email,
    entityId: payload.entityId,
    roleId: payload.roleId,
    status: payload.status,
  });
  return response.data?.data;
};

export const userService = {
  createUser,
  getUsers,
  updateUser,
};
