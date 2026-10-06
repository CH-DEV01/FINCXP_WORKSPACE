import React, { createContext, useContext, useState, useMemo, useCallback } from "react";

const AGREEMENT_KEY = "agreement";

const AgreementContext = createContext();

const readStoredAgreement = () => {
  const storedAgreement = sessionStorage.getItem(AGREEMENT_KEY);
  if (!storedAgreement) return null;
  try {
    return JSON.parse(storedAgreement);
  } catch (error) {
    console.error("Error parsing stored agreement:", error);
    sessionStorage.removeItem(AGREEMENT_KEY);
    return null;
  }
};

export const AgreementProvider = ({ children }) => {
  const [agreement, setAgreement] = useState(readStoredAgreement);

  const saveAgreement = useCallback((agreementData) => {
    setAgreement(agreementData);
    sessionStorage.setItem(AGREEMENT_KEY, JSON.stringify(agreementData));
  }, []);

  const value = useMemo(() => ({ agreement, saveAgreement }), [agreement, saveAgreement]);

  return <AgreementContext.Provider value={value}>{children}</AgreementContext.Provider>;
};

export const useAgreement = () => {
  const context = useContext(AgreementContext);
  if (context === undefined) {
    throw new Error("useAgreement must be used within an AgreementProvider");
  }
  return context;
};
