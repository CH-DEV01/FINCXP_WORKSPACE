import React from "react";
import { formatMoney as formatNumber } from "../../utils/format";

function formatISODate(isoString) {
  // "YYYY-MM-DD" se interpreta como medianoche UTC y en UTC-6 retrocede un día.
  const date =
    typeof isoString === "string" && /^\d{4}-\d{2}-\d{2}$/.test(isoString)
      ? new Date(`${isoString}T00:00:00`)
      : new Date(isoString);
  if (isNaN(date)) {
    return "Fecha inválida";
  }
  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();
  return `${day}/${month}/${year}`;
}

const ItemsSelectedCard = ({ accountsPayable }) => {
  return (
    <div className="flex flex-col h-[min(450px,60vh)] sm:h-[450px] lg:h-full lg:min-h-0 bg-white rounded-lg shadow-md overflow-hidden">
      {accountsPayable && accountsPayable.length > 0 ? (
        <>
          {/* Vista móvil: cards */}
          <div className="md:hidden flex-1 overflow-y-auto p-3 space-y-3">
            {accountsPayable.map((item) => (
              <div key={item.documentNumber} className="rounded-lg border border-gray-200 p-3 space-y-2">
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-gray-500">Emisión</span>
                  <span className="font-medium text-gray-800">{formatISODate(item.issueDate)}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-gray-500">Vencimiento</span>
                  <span className="font-medium text-gray-800">{formatISODate(item.dueDate || item.cutOffDate)}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-gray-500">Días financiamiento</span>
                  <span className="font-medium text-gray-800">{item.financingDays}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm border-t border-gray-100 pt-2">
                  <span className="text-gray-500">Monto solicitado</span>
                  <span className="font-semibold text-gray-900">$ {formatNumber(item.amount)}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-gray-500">Intereses</span>
                  <span className="text-gray-800">$ {formatNumber(item.interests || 0)}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-gray-500">Comisión (IVA incluido)</span>
                  <span className="text-gray-800">$ {formatNumber(item.commissions || 0)}</span>
                </div>
                <div className="flex justify-between gap-2 text-sm">
                  <span className="text-gray-500">Monto a abonar</span>
                  <span className="font-semibold text-gray-900">$ {formatNumber(item.amountToBeDisbursed || 0)}</span>
                </div>
              </div>
            ))}
          </div>

          {/* Vista desktop: tabla */}
          <div className="hidden md:block flex-1 overflow-auto">
            <table className="min-w-[960px] w-full">
              <thead className="bg-gray-50 sticky top-0 z-10">
                <tr>
                  <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Fecha de emisión
                  </th>
                  <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Fecha de vencimiento
                  </th>
                  <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Días de financiamiento
                  </th>
                  <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Monto solicitado
                  </th>
                  <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Intereses
                  </th>
                  <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Comisión (IVA incluido)
                  </th>
                  <th className="px-4 lg:px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    Monto a abonar
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {accountsPayable.map((item) => (
                  <tr key={item.documentNumber} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatISODate(item.issueDate)}
                    </td>
                    <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      {formatISODate(item.dueDate || item.cutOffDate)}
                    </td>
                    <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm text-gray-500">{item.financingDays}</td>
                    <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      $ {formatNumber(item.amount)}
                    </td>
                    <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      $ {formatNumber(item.interests || 0)}
                    </td>
                    <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                      $ {formatNumber(item.commissions || 0)}
                    </td>
                    <td className="px-4 lg:px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                      $ {formatNumber(item.amountToBeDisbursed || 0)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <div className="flex items-center justify-center h-full p-4">
          <p className="text-gray-500 text-center px-4 text-sm">No ha seleccionado ninguna factura para financiar.</p>
        </div>
      )}
    </div>
  );
};

export default ItemsSelectedCard;
