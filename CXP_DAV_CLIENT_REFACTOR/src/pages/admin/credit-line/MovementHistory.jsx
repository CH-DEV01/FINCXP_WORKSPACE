import React from "react";
import Icon from "@mdi/react";
import { mdiHistory } from "@mdi/js";
import Table from "../../../components/Table";
import { formatMoney } from "../../../utils/format";
import { getMovementStyle } from "./creditLineConfig";

const MovementTypeChip = ({ type }) => (
  <span className={`inline-flex px-2 py-0.5 rounded-full text-[10px] font-semibold ${getMovementStyle(type).chip}`}>
    {getMovementStyle(type).label}
  </span>
);

const columns = [
  {
    header: "Referencia",
    accessor: "reference",
    render: (reference) => (
      <span className="block font-bold text-slate-700 md:max-w-[14rem] md:truncate break-all" title={reference}>
        {reference}
      </span>
    ),
  },
  {
    header: "Tipo",
    accessor: "type",
    render: (type) => <MovementTypeChip type={type} />,
  },
  {
    header: "Fecha aplicación",
    accessor: "date",
    render: (date) => new Date(date).toLocaleString("es-SV"),
  },
  {
    header: "Operador",
    accessor: "operator",
    render: (operator) => (
      <span className="block md:max-w-[10rem] md:truncate" title={operator}>
        {operator}
      </span>
    ),
  },
  {
    header: "Monto",
    accessor: "amount",
    render: (amount, item) => (
      <span className={`font-bold font-mono ${getMovementStyle(item.type).amountClass}`}>
        {getMovementStyle(item.type).sign} $ {formatMoney(amount)}
      </span>
    ),
  },
];

// El hook pagina desde 0 (como el servidor); Table pagina desde 1.
const MovementHistory = ({ movements, page, totalPages, onPageChange }) => (
  <div className="w-full flex flex-col gap-3">
    <div className="flex items-center gap-2">
      <Icon path={mdiHistory} size={1} className="text-slate-400 shrink-0" />
      <h4 className="text-xs sm:text-sm font-bold text-slate-700 uppercase tracking-wider">
        Historial de movimientos del cupo
      </h4>
    </div>

    <Table
      columns={columns}
      data={movements}
      currentPage={page + 1}
      totalPages={totalPages}
      onPageChange={(nextPage) => onPageChange(nextPage - 1)}
    />
  </div>
);

export default MovementHistory;
