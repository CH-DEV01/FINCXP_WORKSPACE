import React from "react";

const IconCalendarCheck = () => (
  <svg
    className="h-6 w-6 text-red-600"
    xmlns="http://www.w3.org/2000/svg"
    fill="none"
    viewBox="0 0 24 24"
    strokeWidth={1.5}
    stroke="currentColor"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5m-12-3 2.25 2.25L15 12.75"
    />
  </svg>
);

const IconChecklist = () => (
  <svg
    className="h-6 w-6 text-red-600"
    xmlns="http://www.w3.org/2000/svg"
    fill="none"
    viewBox="0 0 24 24"
    strokeWidth={1.5}
    stroke="currentColor"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
    />
  </svg>
);

const IconBank = () => (
  <svg
    className="h-6 w-6 text-red-600"
    xmlns="http://www.w3.org/2000/svg"
    fill="none"
    viewBox="0 0 24 24"
    strokeWidth={1.5}
    stroke="currentColor"
  >
    <path
      strokeLinecap="round"
      strokeLinejoin="round"
      d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.332A48.36 48.36 0 0012 9.75c-2.551 0-5.056.2-7.5.582V21M3 21h18M12 6.75h.008v.008H12V6.75z"
    />
  </svg>
);

const accountValue = ({ accountNumber, isLoadingAccount, accountError }) => {
  if (isLoadingAccount) return "Cargando…";
  if (accountError) return "No se pudo consultar";
  return accountNumber || "Sin cuenta registrada";
};

const SummaryItem = ({ icon, label, value, wrapValue = false }) => (
  <div className="flex items-center gap-3 min-w-0">
    <div className="shrink-0">{icon}</div>
    <div className="min-w-0">
      <p className="text-xs font-medium text-gray-500">{label}</p>
      <p className={`text-sm font-bold text-gray-900 ${wrapValue ? "break-all" : "truncate"}`}>{value}</p>
    </div>
  </div>
);

// "15:00" o "15:00:00" -> "3:00 p. m."
const formatCutoffTime = (value) => {
  const [hours, minutes] = String(value).split(":").map(Number);
  if (Number.isNaN(hours) || Number.isNaN(minutes)) return null;
  const suffix = hours < 12 ? "a. m." : "p. m.";
  const hour12 = hours % 12 === 0 ? 12 : hours % 12;
  return `${hour12}:${String(minutes).padStart(2, "0")} ${suffix}`;
};

const SelectionSummaryCard = ({
  disbursementDate,
  cutoffTime,
  selectedCount,
  accountNumber,
  isLoadingAccount,
  accountError,
}) => {
  const formattedDate = disbursementDate
    ? disbursementDate.toLocaleDateString("es-ES", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      })
    : "No disponible";
  const formattedCutoff = cutoffTime ? formatCutoffTime(cutoffTime) : null;
  const isAccountMissing = !isLoadingAccount && !accountNumber;

  return (
    <div className="bg-white rounded-lg shadow-lg font-montserrat w-full p-4 sm:px-6">
      <div className="grid grid-cols-2 gap-4 sm:divide-x sm:divide-gray-200">
        <SummaryItem icon={<IconCalendarCheck />} label="Fecha de desembolso" value={formattedDate} />
        <div className="sm:pl-4">
          <SummaryItem icon={<IconChecklist />} label="Documentos seleccionados" value={selectedCount} />
        </div>
      </div>
      <div className="mt-3 pt-3 border-t border-gray-100">
        <div className="flex justify-center">
          <SummaryItem
            icon={<IconBank />}
            label="Cuenta de abono"
            value={accountValue({ accountNumber, isLoadingAccount, accountError })}
            wrapValue
          />
        </div>
        {isAccountMissing && (
          <p
            role="alert"
            className="mt-2 text-center text-xs font-medium text-red-700 bg-red-50 border border-red-200 rounded-md px-3 py-2"
          >
            {accountError
              ? "No se pudo verificar su cuenta de abono. Recargue la página para intentarlo de nuevo."
              : "No tiene una cuenta de abono registrada. Comuníquese con el banco para registrarla antes de solicitar el desembolso."}
          </p>
        )}
      </div>
      {formattedCutoff && (
        <p className="mt-3 pt-3 border-t border-gray-100 text-xs text-gray-500">
          Solicitudes después de las {formattedCutoff} se consideran recibidas el siguiente día hábil.
        </p>
      )}
    </div>
  );
};

export default SelectionSummaryCard;
