const ONE_DAY_MS = 24 * 60 * 60 * 1000;

export const EMPTY_CALCULATION = {
  amount: 0,
  interests: 0,
  commissions: 0,
  amountToFinance: 0,
  amountToBeDisbursed: 0,
  details: [],
};

// Las fechas llegan como "YYYY-MM-DD"; se fija medianoche local para evitar
// el corrimiento de un día que produce new Date("YYYY-MM-DD") (UTC).
export const parseLocalDate = (isoDate) => (isoDate ? new Date(`${isoDate}T00:00:00`) : null);

const daysBetween = (a, b) => {
  const utcA = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate());
  const utcB = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate());
  return Math.round((utcB - utcA) / ONE_DAY_MS);
};

export const mapDocumentToAccount = (doc) => ({
  id: doc.id,
  documentNumber: doc.documentNumber || doc.controlNumber || "",
  amount: Number(doc.nominalAmount) || 0,
  issueDate: parseLocalDate(doc.issueDate),
  dueDate: parseLocalDate(doc.dueDate),
  checked: true,
});

export const toCalculation = (data) => ({
  amount: Number(data?.amount) || 0,
  interests: Number(data?.interests) || 0,
  commissions: Number(data?.commissions) || 0,
  amountToFinance: Number(data?.amountToFinance) || 0,
  amountToBeDisbursed: Number(data?.amountToBeDisbursed) || 0,
  details: (data?.detail || []).map((item) => ({
    ...item,
    // ItemsSelectedCard muestra la fecha de corte como cutOffDate
    cutOffDate: item.dueDate || item.cutOffDate,
    amount: Number(item.amount) || 0,
    interests: Number(item.interests) || 0,
    commissions: Number(item.commissions) || 0,
    amountToFinance: Number(item.amountToFinance) || 0,
    amountToBeDisbursed: Number(item.amountToBeDisbursed) || 0,
  })),
});

// El backend ya excluye los documentos por vencer; este cálculo solo cubre
// el caso en que la fecha cambie mientras la pantalla sigue abierta.
export const getDueInfo = (item) => {
  const issueDate = new Date(item.issueDate);
  const dueDate = item.dueDate ? new Date(item.dueDate) : null;
  const daysToDue = dueDate ? daysBetween(new Date(), dueDate) : Number.POSITIVE_INFINITY;
  return { issueDate, dueDate, isDisabled: daysToDue <= 5 };
};

export const formatDueDate = (dueDate) =>
  dueDate
    ? dueDate.toLocaleDateString("es-ES", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      })
    : "N/A";
