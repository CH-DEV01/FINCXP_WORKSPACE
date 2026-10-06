import React from "react";

// Componente reutilizable para mostrar un par de Etiqueta/Valor
const DataItem = ({ label, value }) => {
  return (
    <div className="flex justify-center w-full">
      <div className="text-center min-w-0">
        <p className="text-xs font-medium text-gray-500 uppercase tracking-wider">{label}</p>
        <p className="text-sm font-bold text-gray-900 break-words mt-0.5">{value}</p>
      </div>
    </div>
  );
};

const PayerData = ({ payer }) => {
  return (
    <div className="bg-white rounded-lg shadow-lg font-montserrat w-full mx-auto p-5">
      <div className="pb-3 border-b border-gray-200 text-center mb-4">
        <h2 className="font-montserrat text-sm font-bold text-gray-800">Detalles del Pagador</h2>
      </div>

      <div className="flex flex-col space-y-4">
        <DataItem label="Nombre" value={payer?.name || "Sin seleccionar"} />
        <DataItem label="NIT" value={payer?.nit || "-"} />
        <DataItem label="Código" value={payer?.code || "-"} />
      </div>
    </div>
  );
};

export default PayerData;
