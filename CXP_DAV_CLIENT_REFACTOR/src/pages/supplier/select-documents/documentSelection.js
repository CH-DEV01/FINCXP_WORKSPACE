export const EMPTY_CALCULATION = {
  amount: 0,
  interests: 0,
  commissions: 0,
  amountToFinance: 0,
  amountToBeDisbursed: 0,
  // "YYYY-MM-DD" tal como lo devuelve el backend; se reenvía al confirmar la solicitud.
  disbursementDate: null,
  details: [],
};

// Las fechas llegan como "YYYY-MM-DD"; se fija medianoche local para evitar
// el corrimiento de un día que produce new Date("YYYY-MM-DD") (UTC).
export const parseLocalDate = (isoDate) => (isoDate ? new Date(`${isoDate}T00:00:00`) : null);

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
  disbursementDate: data?.disbursementDate || null,
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

export const formatDueDate = (dueDate) =>
  dueDate
    ? dueDate.toLocaleDateString("es-ES", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      })
    : "N/A";
