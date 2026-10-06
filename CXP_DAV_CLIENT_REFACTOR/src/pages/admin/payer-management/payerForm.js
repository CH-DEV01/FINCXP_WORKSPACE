export const DEFAULT_FORM_VALUES = {
  name: "",
  nit: "",
  accountNumber: "",
  creditFacilityNumber: "",
  approvedAmount: "",
  interestRate: "",
  commissionRate: "",
  calculationBase: "COMERCIAL_360",
  status: "ACTIVE",
  warningThreshold: "80",
  consumedAmount: "",
  consumptionReference: "",
};

export const CALCULATION_BASE_OPTIONS = [
  { value: "COMERCIAL_360", label: "Comercial (360 días)" },
  { value: "CALENDARIO_365", label: "Calendario (365 días)" },
];

export const CALCULATION_BASE_LABELS = Object.fromEntries(
  CALCULATION_BASE_OPTIONS.map(({ value, label }) => [value, label]),
);

export const formatNIT = (value) => {
  if (!value) return "";
  const digits = value.replace(/\D/g, "");

  let formatted = "";
  if (digits.length > 0) formatted += digits.slice(0, 4);
  if (digits.length > 4) formatted += "-" + digits.slice(4, 10);
  if (digits.length > 10) formatted += "-" + digits.slice(10, 13);
  if (digits.length > 13) formatted += "-" + digits.slice(13, 14);

  return formatted;
};

// Las tasas y el umbral llegan como fracción decimal (0.155 = 15.5 %)
export const formatFractionAsPercent = (value) =>
  value === null || value === undefined ? "—" : `${Number((Number(value) * 100).toFixed(4))} %`;

export const percentToFraction = (value) => Number((Number(value) / 100).toFixed(6));

const fractionToPercentInput = (value, decimals = 4) =>
  value === null || value === undefined ? "" : String(Number((Number(value) * 100).toFixed(decimals)));

export const toPayerFormValues = (payer) => ({
  name: payer.name || "",
  nit: payer.nit || "",
  accountNumber: payer.accountNumber || "",
  creditFacilityNumber: payer.creditFacilityNumber || "",
  approvedAmount: payer.facilityLimitAmount != null ? String(payer.facilityLimitAmount) : "",
  consumedAmount: payer.amountInUse != null ? String(payer.amountInUse) : "",
  consumptionReference: "",
  interestRate: fractionToPercentInput(payer.interestRate),
  commissionRate: fractionToPercentInput(payer.commissionRate),
  calculationBase: payer.calculationBase || DEFAULT_FORM_VALUES.calculationBase,
  status: payer.status || DEFAULT_FORM_VALUES.status,
  warningThreshold:
    payer.warningThresholdPercentage != null
      ? fractionToPercentInput(payer.warningThresholdPercentage, 0)
      : DEFAULT_FORM_VALUES.warningThreshold,
});
