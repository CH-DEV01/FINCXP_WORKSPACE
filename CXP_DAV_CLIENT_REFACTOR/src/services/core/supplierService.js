import api from "../api";

/** Cuenta principal del proveedor autenticado, a la que se abona el desembolso (accountNumber es null si no tiene). */
const getOwnBankAccount = async () => {
  const response = await api.get("/v1/suppliers/me/bank-account");
  return response.data?.data;
};

export const supplierService = {
  getOwnBankAccount,
};
