import React, { useState } from "react";
import Icon from "@mdi/react";
import { mdiCashFast, mdiKeyboardReturn } from "@mdi/js";
import FinancingSummaryCard from "../../components/Home/FinancingSummaryCard.jsx";
import WelcomeBannerCard from "../../components/Home/WelcomeBannerCard.jsx";
import SelectionSummaryCard from "../../components/Home/SelectionSummaryCard.jsx";
import ItemsSelectedCard from "../../components/Home/ItemsSelectedCard.jsx";
import { useAgreement } from "../../context/AgreementContext.jsx";
import { fundingRequestService } from "../../services/core/fundingRequestService.js";
import TermsAcceptanceModal from "../../components/terms/TermsAcceptanceModal.jsx";
import SupplierData from "../../components/SupplierData.jsx";
import { formatMoney } from "../../utils/format.js";
import { apiErrorMessage, showError, showLoading, showSuccess } from "../../utils/errors.js";
import PayableDocumentsList from "./select-documents/PayableDocumentsList.jsx";
import useDocumentSelection from "./select-documents/useDocumentSelection.js";
import useSupplierBankAccount from "../../hooks/useSupplierBankAccount.js";

const ITEMS_PER_PAGE = 10;

const SelectDocuments = () => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [step, setStep] = useState(1);

  const { agreement } = useAgreement();
  const masterAgreementId = agreement?.id;

  const {
    accountsPayable,
    isLoadingDocuments,
    currentPage,
    setCurrentPage,
    selectedIds,
    toggleDocument,
    disbursementDate,
    cutoffTime,
    calculation,
    calculateNow,
    refreshDocuments,
  } = useDocumentSelection(masterAgreementId);

  const supplierName = agreement?.supplierName || "N/A";
  const payerName = agreement?.PayerName || agreement?.payerName || "N/A";
  const paymentPolicyDays = agreement?.paymentPolicyDays;
  const paymentPolicyLabel = paymentPolicyDays != null ? `Pago a ${paymentPolicyDays} días` : "N/A";
  const disbursementPolicyLabel = agreement?.disbursementPolicyName || "N/A";

  const { accountNumber, isLoading: isLoadingAccount, hasError: accountError } = useSupplierBankAccount();

  const hasAccount = Boolean(accountNumber);
  const hasSelection = selectedIds.length > 0;
  const canContinue = hasSelection && hasAccount;

  const nextStep = async () => {
    await calculateNow();
    setStep(2);
  };

  const prevStep = () => {
    setStep(1);
  };

  const submitFundingRequest = async (termVersionId) => {
    if (selectedIds.length === 0) {
      throw new Error("No hay elementos seleccionados para solicitar");
    }

    if (!masterAgreementId) {
      throw new Error("Se perdió la referencia al convenio marco.");
    }

    if (!termVersionId) {
      throw new Error("No se encontró la versión activa de términos y condiciones.");
    }

    return fundingRequestService.submitFunding({
      masterAgreementId,
      documentIds: selectedIds,
      termVersionId,
    });
  };

  const save = async (termVersionId) => {
    try {
      showLoading("", "Procesando solicitud");

      await submitFundingRequest(termVersionId);

      setStep(1);
      await refreshDocuments();

      showSuccess("La solicitud de desembolso se ha efectuado correctamente", "Éxito");
    } catch (error) {
      await showError(apiErrorMessage(error, "Ocurrió un error al procesar la solicitud"));

      if (error?.response?.status === 422) {
        setStep(1);
        refreshDocuments();
      }
    }
  };

  const handleAccept = (acceptedVersion) => {
    setIsModalOpen(false);
    save(acceptedVersion?.id);
  };

  return (
    <div className="flex flex-col lg:grid lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)] lg:items-stretch gap-3 sm:gap-4 rounded-lg w-full lg:h-[calc(100dvh-5.5rem)] lg:min-h-0 lg:overflow-hidden pb-3">
      <div className="w-full flex flex-col min-w-0 min-h-0 gap-3 sm:gap-4 lg:h-full">
        <div className="flex w-full bg-gray-200 p-2 rounded-lg shrink-0">
          <WelcomeBannerCard />
        </div>

        <div className="flex flex-col flex-1 min-h-0">
          {step === 1 && (
            <PayableDocumentsList
              documents={accountsPayable}
              isLoading={isLoadingDocuments}
              currentPage={currentPage}
              pageSize={ITEMS_PER_PAGE}
              onPageChange={setCurrentPage}
              onToggle={toggleDocument}
            />
          )}

          {step === 2 && (
            <div className="flex flex-col gap-4 h-full min-h-0">
              <div className="flex-1 min-h-0">
                <ItemsSelectedCard accountsPayable={calculation.details} />
              </div>
              <div className="border-t border-gray-200 pt-4 shrink-0">
                <button
                  type="button"
                  onClick={prevStep}
                  className="cursor-pointer w-full flex justify-center items-center gap-2 py-2.5 sm:py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                >
                  <Icon path={mdiKeyboardReturn} size={1} color="gray" />
                  Regresar
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="w-full rounded-lg flex flex-col bg-gray-200 p-2 gap-2 shrink-0 lg:h-full lg:min-h-0">
        <div className="space-y-2 lg:flex-1 lg:min-h-0 lg:overflow-y-auto lg:pr-1">
          <SupplierData
            supplierName={supplierName}
            payerName={payerName}
            paymentPolicy={paymentPolicyLabel}
            disbursementPolicy={disbursementPolicyLabel}
            availableDocuments={accountsPayable.length}
          />
          <SelectionSummaryCard
            disbursementDate={disbursementDate}
            cutoffTime={cutoffTime}
            selectedCount={selectedIds.length}
            accountNumber={accountNumber}
            isLoadingAccount={isLoadingAccount}
            accountError={accountError}
          />
          <FinancingSummaryCard
            totalAmount={formatMoney(calculation.amount)}
            interests={formatMoney(calculation.interests)}
            commission={formatMoney(calculation.commissions)}
            amountToBePaid={formatMoney(calculation.amountToBeDisbursed)}
          />
        </div>

        <div className="mt-auto shrink-0">
          {step === 1 && (
            <button
              disabled={!canContinue}
              onClick={nextStep}
              className={`cursor-pointer w-full py-2.5 sm:py-2 px-4 rounded-md shadow-lg text-sm font-medium text-white ${
                canContinue ? "bg-red-800 hover:bg-red-700" : "bg-red-500 opacity-50 cursor-not-allowed"
              }`}
            >
              Ver detalles
            </button>
          )}

          {step === 2 && (
            <button
              disabled={!hasAccount}
              onClick={() => setIsModalOpen(true)}
              className="cursor-pointer w-full flex justify-center items-center gap-2 py-2.5 sm:py-2 px-4 bg-red-800 text-white rounded-md shadow-sm text-sm font-medium hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Solicitar desembolso
              <Icon path={mdiCashFast} size={1} color="white" />
            </button>
          )}
        </div>
      </div>

      <TermsAcceptanceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        termTypeCode="SUPPLIER_TERM_TYPE"
        confirmLabel="Confirmar solicitud"
        onConfirm={handleAccept}
      />
    </div>
  );
};

export default SelectDocuments;
