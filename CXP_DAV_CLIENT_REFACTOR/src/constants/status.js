export const ENTITY_STATUS = {
  ACTIVE: { label: "Activo", className: "bg-green-100 text-green-700" },
  INACTIVE: { label: "Inactivo", className: "bg-gray-200 text-gray-600" },
};

export const STATUS_OPTIONS = [
  { value: "ACTIVE", label: ENTITY_STATUS.ACTIVE.label },
  { value: "INACTIVE", label: ENTITY_STATUS.INACTIVE.label },
];

export const BADGE_CLASS = "inline-flex px-2 py-0.5 rounded-full text-[10px] font-semibold";

export const USER_STATUS = {
  ACTIVE: { label: "Activo", className: "bg-green-100 text-green-800 border-green-200", dot: "bg-green-500" },
  INACTIVE: { label: "Inactivo", className: "bg-gray-100 text-gray-700 border-gray-200", dot: "bg-gray-400" },
};

export const USER_BADGE_CLASS =
  "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider border";

/** Estados de documento en el orden en que se muestran en filtros y resúmenes. */
export const DOCUMENT_STATUSES = [
  { id: "APPROVED", label: "Cargado", className: "bg-green-100 text-green-800", dot: "bg-green-500" },
  { id: "REQUESTED_FOR_FINANCING", label: "Solicitado", className: "bg-red-100 text-red-800", dot: "bg-red-500" },
  {
    id: "REQUESTED_FOR_DISBURSEMENT",
    label: "En proceso",
    className: "bg-orange-100 text-orange-800",
    dot: "bg-orange-500",
  },
  { id: "DISBURSED", label: "Desembolsado", className: "bg-yellow-100 text-yellow-800", dot: "bg-yellow-500" },
  { id: "IN_QUARANTINE", label: "No financiable", className: "bg-gray-200 text-gray-800", dot: "bg-gray-500" },
  { id: "INACTIVATED_BY_PAYER", label: "Inactivo", className: "bg-slate-300 text-slate-900", dot: "bg-slate-700" },
  { id: "REQUESTED_FOR_DISPERSION", label: "En dispersión", className: "bg-sky-100 text-sky-800", dot: "bg-sky-500" },
  { id: "DISPERSED", label: "Dispersado", className: "bg-indigo-100 text-indigo-800", dot: "bg-indigo-500" },
];

/** Estados que el proveedor no ve en su bitácora: documentos que no llegaron a financiarse. */
export const SUPPLIER_HIDDEN_DOCUMENT_STATUS_IDS = [
  "IN_QUARANTINE",
  "INACTIVATED_BY_PAYER",
  "REQUESTED_FOR_DISPERSION",
  "DISPERSED",
];

export const DOCUMENT_STATUS = Object.fromEntries(DOCUMENT_STATUSES.map((status) => [status.id, status]));

export const DOCUMENT_BADGE_CLASS =
  "inline-flex items-center px-2.5 sm:px-3 py-1 rounded-full text-[10px] sm:text-xs font-semibold leading-4 uppercase";

export const TERM_VERSION_STATUS = {
  ACTIVE: { label: "Vigente", className: "bg-green-50 text-green-700 border-green-200" },
  DRAFT: { label: "Borrador", className: "bg-amber-50 text-amber-700 border-amber-200" },
  INACTIVE: { label: "Histórica", className: "bg-gray-100 text-gray-600 border-gray-200" },
};

export const TERM_BADGE_CLASS = "inline-flex px-2 py-0.5 rounded-full text-[11px] font-semibold border";

/** Estado de una solicitud de desembolso en la terminal y la bitácora de lotes. */
export const REQUEST_STATUS = {
  INGRESADO: { label: "Ingresado", className: "bg-slate-100 text-slate-700" },
  EN_PROCESO: { label: "En proceso", className: "bg-amber-50 text-amber-700" },
  DESEMBOLSADO: { label: "Desembolsado", className: "bg-emerald-50 text-emerald-700" },
};

/** Estado de una solicitud de dispersión en la terminal y la bitácora de dispersiones. */
export const DISPERSION_REQUEST_STATUS = {
  INGRESADO: { ...REQUEST_STATUS.INGRESADO, label: "Generado" },
  EN_PROCESO: REQUEST_STATUS.EN_PROCESO,
  DISPERSADO: { label: "Dispersado", className: "bg-indigo-50 text-indigo-700" },
};

export const REQUEST_BADGE_CLASS = "inline-block text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-md";
