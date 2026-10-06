const moneyFormatter = new Intl.NumberFormat("en-US", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
});

/** 1234.5 -> "1,234.50"; null, undefined o texto no numérico se muestran como 0.00. */
export const formatMoney = (value) => moneyFormatter.format(Number(value) || 0);

/** 1234.5 -> "$1,234.50"; sin valor -> "—". */
export const formatCurrency = (value) =>
  value === null || value === undefined ? "—" : currencyFormatter.format(Number(value) || 0);

/** "YYYY-MM-DD" (o un instante ISO) -> "DD/MM/YYYY" sin pasar por Date, para no correr el día por zona horaria. */
export const formatDate = (isoDate, fallback = "-") => {
  if (!isoDate) return fallback;
  const [year, month, day] = String(isoDate).slice(0, 10).split("-");
  return `${day}/${month}/${year}`;
};

export const formatDateTime = (instant) => (instant ? new Date(instant).toLocaleString() : "-");

export const formatPercent = (value) => `${Number(value || 0).toFixed(2)}%`;

/** Las tasas llegan como fracción (0.155 -> "15.50%"). */
export const formatRate = (fraction) =>
  fraction !== null && fraction !== undefined ? `${(Number(fraction) * 100).toFixed(2)}%` : "N/D";

/** Máscara de monto para inputs: "1500.5" -> "1,500.5" (con prefijo opcional, p. ej. "$ "). */
export const formatAmountMask = (value, prefix = "") => {
  if (value === null || value === undefined || value === "") return "";
  const [integer, decimal] = String(value).split(".");
  const grouped = integer.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  return `${prefix}${grouped}${decimal !== undefined ? `.${decimal.slice(0, 2)}` : ""}`;
};

/** Deja solo dígitos y un punto decimal con máximo dos decimales ("$ 1,500.567" -> "1500.56"). */
export const sanitizeAmount = (value) => {
  let clean = String(value).replace(/[^0-9.]/g, "");
  const parts = clean.split(".");
  if (parts.length > 2) clean = `${parts[0]}.${parts.slice(1).join("")}`;
  const [integer, decimal] = clean.split(".");
  return decimal !== undefined ? `${integer}.${decimal.slice(0, 2)}` : integer;
};

export const downloadBlob = (data, fileName, type = "application/pdf") => {
  const blob = new Blob([data], { type });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.setAttribute("download", fileName);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};
