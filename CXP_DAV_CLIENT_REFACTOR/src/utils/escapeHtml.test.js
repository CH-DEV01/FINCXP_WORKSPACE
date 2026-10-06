import { describe, expect, it } from "vitest";
import escapeHtml from "./escapeHtml.js";

describe("escapeHtml", () => {
  it("escapa los caracteres especiales de HTML", () => {
    expect(escapeHtml(`<img src="x" onerror='alert(1)'>&`)).toBe(
      "&lt;img src=&quot;x&quot; onerror=&#39;alert(1)&#39;&gt;&amp;",
    );
  });

  it("no escapa dos veces lo que ya es una entidad literal", () => {
    expect(escapeHtml("&amp;")).toBe("&amp;amp;");
  });

  it("deja intacto el texto normal, incluidos acentos", () => {
    expect(escapeHtml("Pagador Ñandú, S.A. de C.V.")).toBe("Pagador Ñandú, S.A. de C.V.");
  });

  it("convierte valores no textuales a cadena", () => {
    expect(escapeHtml(42)).toBe("42");
    expect(escapeHtml(null)).toBe("");
    expect(escapeHtml(undefined)).toBe("");
  });
});
