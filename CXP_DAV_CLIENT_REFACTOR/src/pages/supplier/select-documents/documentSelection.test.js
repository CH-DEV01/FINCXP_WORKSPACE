import { describe, expect, it } from "vitest";
import {
  EMPTY_CALCULATION,
  formatDueDate,
  mapDocumentToAccount,
  parseLocalDate,
  toCalculation,
} from "./documentSelection";

describe("toCalculation", () => {
  it("keeps the quoted disbursement date so it can be sent back when submitting", () => {
    const calculation = toCalculation({
      amount: "1000.00",
      amountToBeDisbursed: "986.98",
      disbursementDate: "2026-10-15",
      detail: [],
    });

    expect(calculation.disbursementDate).toBe("2026-10-15");
    expect(calculation.amountToBeDisbursed).toBe(986.98);
    expect(EMPTY_CALCULATION.disbursementDate).toBeNull();
  });
});

describe("mapDocumentToAccount", () => {
  it("marks every financeable document as checked so the selection matches the backend list", () => {
    const account = mapDocumentToAccount({
      id: "doc-1",
      documentNumber: null,
      controlNumber: "DTE-001",
      nominalAmount: "1500.50",
      issueDate: "2026-10-01",
      dueDate: "2026-10-10",
    });

    expect(account).toEqual({
      id: "doc-1",
      documentNumber: "DTE-001",
      amount: 1500.5,
      issueDate: parseLocalDate("2026-10-01"),
      dueDate: parseLocalDate("2026-10-10"),
      checked: true,
    });
  });
});

describe("formatDueDate", () => {
  it("formats a local date or falls back to N/A", () => {
    expect(formatDueDate(null)).toBe("N/A");
    expect(formatDueDate(parseLocalDate("2026-10-15"))).toMatch(/15/);
  });
});
