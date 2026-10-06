import React from "react";

const FinancingSummaryCard = ({ totalAmount, interests, commission, amountToBePaid }) => {
  return (
    <div>
      <div className="bg-white p-4 sm:px-6 rounded-lg shadow-lg">
        <div className="flex flex-wrap items-center justify-between w-full gap-x-3 gap-y-1 mb-3">
          <h2 className="text-sm sm:text-md font-bold text-gray-800">Monto solicitado</h2>

          <h2 className="text-sm text-gray-800 text-end font-semibold">$ {totalAmount}</h2>
        </div>

        <div className="border-t border-b border-gray-200 py-2">
          <div className="flex justify-between gap-3 py-1.5 text-sm">
            <span className="text-gray-700">Intereses</span>
            <span className="font-semibold shrink-0">$ {interests}</span>
          </div>
          <div className="flex justify-between gap-3 py-1.5 text-sm">
            <span className="text-gray-700">Comisión (IVA incluido)</span>
            <span className="font-semibold shrink-0">$ {commission}</span>
          </div>
        </div>

        <div className="flex justify-between gap-3 py-2.5 mt-2 bg-gray-50 px-3 rounded">
          <span className="text-gray-800 font-bold text-sm sm:text-md">Monto a abonar</span>
          <span className="text-red-600 font-bold shrink-0">$ {amountToBePaid}</span>
        </div>
      </div>
    </div>
  );
};

export default FinancingSummaryCard;
