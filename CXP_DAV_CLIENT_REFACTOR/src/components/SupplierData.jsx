import React from "react";

const IconCalendar = () => (
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
      d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 11.25v7.5"
    />
  </svg>
);

const IconPayer = () => (
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
      d="M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z"
    />
  </svg>
);

const IconCash = () => (
  <svg
    className="h-6 w-6 text-red-600"
    xmlns="http://www.w3.org/2000/svg"
    fill="none"
    viewBox="0 0 24 24"
    strokeWidth={1.5}
    stroke="currentColor"
  >
    <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />
  </svg>
);

const IconDocuments = () => (
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
      d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
    />
  </svg>
);

const DataItem = ({ icon, label, value }) => {
  return (
    <div className="flex items-start justify-start w-full">
      <div className="mt-1 flex-shrink-0">{icon}</div>
      <div className="ml-3 text-left min-w-0">
        <p className="text-sm font-medium text-gray-500">{label}</p>
        <p className="text-xs font-semibold text-gray-900 break-words">{value}</p>
      </div>
    </div>
  );
};

const SupplierData = ({ supplierName, paymentPolicy, disbursementPolicy, payerName, availableDocuments }) => {
  return (
    <div className="bg-white rounded-lg shadow-lg font-montserrat w-full p-4 sm:px-6">
      <div className="pb-3 border-b border-gray-200 text-center">
        <h2 className="font-montserrat text-sm font-bold text-gray-900 break-words">{supplierName || "Proveedor"}</h2>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-3 sm:gap-y-4 pt-3 sm:pt-4">
        <DataItem icon={<IconPayer />} label="Pagador" value={payerName || "N/A"} />

        <DataItem
          icon={<IconDocuments />}
          label="Documentos disponibles"
          value={availableDocuments != null ? String(availableDocuments) : "0"}
        />

        <DataItem icon={<IconCalendar />} label="Política de pago" value={paymentPolicy || "N/A"} />

        <DataItem icon={<IconCash />} label="Día de desembolso" value={disbursementPolicy || "N/A"} />
      </div>
    </div>
  );
};

export default SupplierData;
