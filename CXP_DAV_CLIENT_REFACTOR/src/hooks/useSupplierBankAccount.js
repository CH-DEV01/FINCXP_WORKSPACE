import { useEffect, useState } from "react";
import { supplierService } from "../services/core/supplierService.js";

/** Cuenta de abono del proveedor autenticado. {@code hasError} indica que no se pudo consultar. */
const useSupplierBankAccount = () => {
  const [accountNumber, setAccountNumber] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [hasError, setHasError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    supplierService
      .getOwnBankAccount()
      .then((data) => {
        if (!cancelled) setAccountNumber(data?.accountNumber || null);
      })
      .catch((error) => {
        if (cancelled) return;
        console.error("No se pudo obtener la cuenta bancaria del proveedor:", error);
        setHasError(true);
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return { accountNumber, isLoading, hasError };
};

export default useSupplierBankAccount;
