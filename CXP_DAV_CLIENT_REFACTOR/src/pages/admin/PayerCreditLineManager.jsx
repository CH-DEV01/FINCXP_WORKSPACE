import React, { useState } from "react";
import Swal from "sweetalert2";

import { creditFacilityService } from "../../services/admin/creditFacilityService";
import { formatMoney } from "../../utils/format";
import { apiErrorMessage, showError } from "../../utils/errors";
import CreditLineHeader from "./credit-line/CreditLineHeader";
import CreditLineDetailCard from "./credit-line/CreditLineDetailCard";
import MovementHistory from "./credit-line/MovementHistory";
import RegisterRepaymentModal from "./credit-line/RegisterRepaymentModal";
import usePayerCreditLine from "./credit-line/usePayerCreditLine";
import { EMPTY_CREDIT_LINE } from "./credit-line/creditLineConfig";

const PayerCreditLineManager = () => {
  const {
    payersList,
    activePayer,
    isLoadingPayers,
    selectPayer,
    creditLineData,
    repaymentHistory,
    historyPage,
    setHistoryPage,
    historyTotalPages,
    refresh,
  } = usePayerCreditLine();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isProcessingRepayment, setIsProcessingRepayment] = useState(false);

  const handleExecuteRepayment = async (repaymentPayload) => {
    setIsProcessingRepayment(true);

    try {
      if (!creditLineData || !creditLineData.id) {
        throw new Error("No se ha podido identificar el ID del cupo de crédito.");
      }

      const apiPayload = {
        creditFacilityId: creditLineData.id,
        amount: repaymentPayload.amount,
        reference: repaymentPayload.reference,
        type: repaymentPayload.type,
      };

      await creditFacilityService.restoreCreditFacility(apiPayload);

      setIsModalOpen(false);
      Swal.fire({
        title: "¡Abono aplicado!",
        text: `Se han restaurado $${formatMoney(repaymentPayload.amount)} al cupo de crédito.`,
        icon: "success",
        confirmButtonColor: "#dc2626",
      });

      refresh();
    } catch (error) {
      showError(apiErrorMessage(error, "Ocurrió un error inesperado"), "Transacción rechazada");
    } finally {
      setIsProcessingRepayment(false);
    }
  };

  const data = creditLineData || EMPTY_CREDIT_LINE;

  return (
    <div className="flex flex-col w-full min-w-0 max-w-none font-sans">
      <CreditLineHeader
        payers={payersList}
        activePayer={activePayer}
        onSelectPayer={selectPayer}
        isLoadingPayers={isLoadingPayers}
      />

      <div className="flex flex-col gap-4 sm:gap-6">
        <CreditLineDetailCard data={data} onRegisterRepayment={() => setIsModalOpen(true)} />

        <MovementHistory
          movements={repaymentHistory}
          page={historyPage}
          totalPages={historyTotalPages}
          onPageChange={setHistoryPage}
        />
      </div>

      <RegisterRepaymentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onConfirm={handleExecuteRepayment}
        isProcessing={isProcessingRepayment}
        currentConsumed={data.currentConsumed}
      />
    </div>
  );
};

export default PayerCreditLineManager;
