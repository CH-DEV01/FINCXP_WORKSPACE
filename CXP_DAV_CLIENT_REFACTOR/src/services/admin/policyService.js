import api from "../api";

const getDisbursementPolicies = async () => {
  const response = await api.get("/v1/disbursement-policies");
  return response.data?.data ?? [];
};

const getPaymentPolicies = async () => {
  const response = await api.get("/v1/payment-policies");
  return response.data?.data ?? [];
};

export const policyService = {
  getDisbursementPolicies,
  getPaymentPolicies,
};
