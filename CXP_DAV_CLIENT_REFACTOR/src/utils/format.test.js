import { afterEach, describe, expect, it, vi } from "vitest";
import {
  downloadBlob,
  formatAmountMask,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatMoney,
  formatPercent,
  formatRate,
  sanitizeAmount,
} from "./format.js";

describe("formatMoney", () => {
  it("formatea con separador de miles y dos decimales", () => {
    expect(formatMoney(1234.5)).toBe("1,234.50");
    expect(formatMoney("1000000")).toBe("1,000,000.00");
    expect(formatMoney(0.005)).toBe("0.01");
  });

  it("muestra 0.00 para valores vacíos o no numéricos", () => {
    expect(formatMoney(null)).toBe("0.00");
    expect(formatMoney(undefined)).toBe("0.00");
    expect(formatMoney("abc")).toBe("0.00");
  });
});

describe("formatCurrency", () => {
  it("agrega el símbolo de dólar", () => {
    expect(formatCurrency(1234.5)).toBe("$1,234.50");
    expect(formatCurrency(0)).toBe("$0.00");
  });

  it("devuelve un guion largo cuando no hay valor", () => {
    expect(formatCurrency(null)).toBe("—");
    expect(formatCurrency(undefined)).toBe("—");
  });
});

describe("formatDate", () => {
  it("convierte YYYY-MM-DD a DD/MM/YYYY", () => {
    expect(formatDate("2026-01-31")).toBe("31/01/2026");
  });

  it("toma solo la parte de fecha de un instante ISO sin correr el día", () => {
    expect(formatDate("2026-03-01T23:30:00Z")).toBe("01/03/2026");
  });

  it("usa el valor por defecto cuando no hay fecha", () => {
    expect(formatDate(null)).toBe("-");
    expect(formatDate("", "N/D")).toBe("N/D");
  });
});

describe("formatDateTime", () => {
  it("devuelve '-' sin instante", () => {
    expect(formatDateTime(null)).toBe("-");
  });

  it("devuelve la fecha local para un instante válido", () => {
    const instant = "2026-05-10T15:00:00Z";
    expect(formatDateTime(instant)).toBe(new Date(instant).toLocaleString());
  });
});

describe("formatPercent", () => {
  it("formatea con dos decimales y signo de porcentaje", () => {
    expect(formatPercent(12.345)).toBe("12.35%");
    expect(formatPercent("7")).toBe("7.00%");
    expect(formatPercent(null)).toBe("0.00%");
  });
});

describe("formatRate", () => {
  it("convierte una fracción a porcentaje", () => {
    expect(formatRate(0.155)).toBe("15.50%");
    expect(formatRate(0)).toBe("0.00%");
  });

  it("devuelve N/D sin tasa", () => {
    expect(formatRate(null)).toBe("N/D");
    expect(formatRate(undefined)).toBe("N/D");
  });
});

describe("formatAmountMask", () => {
  it("agrupa miles y conserva los decimales escritos", () => {
    expect(formatAmountMask("1500.5")).toBe("1,500.5");
    expect(formatAmountMask("1234567")).toBe("1,234,567");
    expect(formatAmountMask("1500.")).toBe("1,500.");
  });

  it("recorta a dos decimales y admite prefijo", () => {
    expect(formatAmountMask("1500.567", "$ ")).toBe("$ 1,500.56");
  });

  it("devuelve cadena vacía sin valor", () => {
    expect(formatAmountMask("")).toBe("");
    expect(formatAmountMask(null)).toBe("");
    expect(formatAmountMask(undefined)).toBe("");
  });
});

describe("sanitizeAmount", () => {
  it("elimina símbolos y separadores", () => {
    expect(sanitizeAmount("$ 1,500.567")).toBe("1500.56");
    expect(sanitizeAmount("abc123")).toBe("123");
  });

  it("deja un solo punto decimal", () => {
    expect(sanitizeAmount("1.2.3")).toBe("1.23");
  });

  it("conserva el punto final mientras se escribe", () => {
    expect(sanitizeAmount("10.")).toBe("10.");
  });
});

describe("downloadBlob", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("crea un enlace temporal, lo pulsa y libera la URL", () => {
    window.URL.createObjectURL = vi.fn(() => "blob:fake");
    window.URL.revokeObjectURL = vi.fn();
    const click = vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});

    downloadBlob("contenido", "reporte.pdf");

    expect(window.URL.createObjectURL).toHaveBeenCalledTimes(1);
    const blob = window.URL.createObjectURL.mock.calls[0][0];
    expect(blob).toBeInstanceOf(Blob);
    expect(blob.type).toBe("application/pdf");
    expect(click).toHaveBeenCalledTimes(1);
    const link = click.mock.contexts[0];
    expect(link.getAttribute("download")).toBe("reporte.pdf");
    expect(link.href).toBe("blob:fake");
    expect(document.body.contains(link)).toBe(false);
    expect(window.URL.revokeObjectURL).toHaveBeenCalledWith("blob:fake");
  });
});
