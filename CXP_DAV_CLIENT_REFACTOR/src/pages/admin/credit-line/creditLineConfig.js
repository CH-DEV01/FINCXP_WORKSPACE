import { mdiAlertOctagonOutline, mdiShieldCheckOutline } from "@mdi/js";

const DEFAULT_WARNING_THRESHOLD = 80;

export const EMPTY_CREDIT_LINE = {
  id: "",
  totalLimit: 0,
  currentConsumed: 0,
  availableLimit: 0,
  utilizationRate: 0,
  warningThreshold: DEFAULT_WARNING_THRESHOLD,
  thresholdStatus: "NORMAL",
  interestRate: null,
  commissionRate: null,
  calculationBase: null,
};

export const toCreditLine = (facility) => {
  const limit = facility.facilityLimitAmount || 0;
  const consumed = facility.amountInUse || 0;

  return {
    id: facility.id || "",
    totalLimit: limit,
    currentConsumed: consumed,
    availableLimit: facility.availableAmount || 0,
    utilizationRate:
      facility.utilizationPercentage != null
        ? Number(facility.utilizationPercentage)
        : limit > 0
          ? Number(((consumed / limit) * 100).toFixed(2))
          : 0,
    warningThreshold: Number(facility.warningThresholdPercentage ?? DEFAULT_WARNING_THRESHOLD),
    thresholdStatus: facility.thresholdStatus || "NORMAL",
    interestRate: facility.interestRate,
    commissionRate: facility.commissionRate,
    calculationBase: facility.calculationBase,
  };
};

export const THRESHOLD_STATUS = {
  NORMAL: {
    label: "Normal",
    icon: mdiShieldCheckOutline,
    container: "bg-emerald-50/60 border-emerald-100",
    iconClass: "text-emerald-600",
    chip: "bg-emerald-100 text-emerald-800",
    dot: "bg-emerald-500",
  },
  CRITICAL: {
    label: "Crítico",
    icon: mdiAlertOctagonOutline,
    container: "bg-red-50/60 border-red-100",
    iconClass: "text-red-600",
    chip: "bg-red-100 text-red-800",
    dot: "bg-red-500",
  },
};

export const CALCULATION_BASE_LABELS = {
  COMERCIAL_360: "Base 360 días",
  CALENDARIO_365: "Base 365 días",
};

// INITIAL_BALANCE es un cargo (consumo previo al alta del cupo); PARTIAL/FULL son abonos y
// DOCUMENT_INACTIVATION libera el monto de un documento que pasó a Inactivo.
const MOVEMENT_TYPES = {
  PARTIAL: {
    label: "Abono parcial",
    sign: "+",
    amountClass: "text-emerald-600",
    chip: "bg-emerald-50 text-emerald-700",
  },
  FULL: { label: "Abono total", sign: "+", amountClass: "text-emerald-600", chip: "bg-emerald-50 text-emerald-700" },
  INITIAL_BALANCE: {
    label: "Saldo inicial",
    sign: "−",
    amountClass: "text-amber-600",
    chip: "bg-amber-50 text-amber-700",
  },
  DOCUMENT_INACTIVATION: {
    label: "Documento inactivado",
    sign: "+",
    amountClass: "text-sky-600",
    chip: "bg-sky-50 text-sky-700",
  },
};

export const getMovementStyle = (type) => MOVEMENT_TYPES[type] || MOVEMENT_TYPES.PARTIAL;
