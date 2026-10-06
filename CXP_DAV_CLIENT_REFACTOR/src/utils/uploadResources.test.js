import { describe, expect, it } from "vitest";
import { formatFileSize, MANUAL_RULES, resourceFileError, TEMPLATE_RULES } from "./uploadResources.js";

const file = (name, size) => ({ name, size });
const MB = 1024 * 1024;

describe("resourceFileError con la plantilla", () => {
  it("acepta un .xlsx de hasta 1 MB sin importar mayúsculas", () => {
    expect(resourceFileError(file("Plantilla.XLSX", MB), TEMPLATE_RULES)).toBeNull();
  });

  it("rechaza otras extensiones, archivos vacíos y archivos de más de 1 MB", () => {
    expect(resourceFileError(file("plantilla.xls", 10), TEMPLATE_RULES)).toBe(
      "La plantilla debe ser un archivo .xlsx.",
    );
    expect(resourceFileError(file("plantilla.xlsm", 10), TEMPLATE_RULES)).toBe(
      "La plantilla debe ser un archivo .xlsx.",
    );
    expect(resourceFileError(file("plantilla.xlsx", 0), TEMPLATE_RULES)).toBe("La plantilla está vacía.");
    expect(resourceFileError(file("plantilla.xlsx", MB + 1), TEMPLATE_RULES)).toBe(
      "La plantilla supera el tamaño máximo permitido de 1 MB.",
    );
    expect(resourceFileError(null, TEMPLATE_RULES)).toBe("Seleccione la plantilla.");
  });
});

describe("resourceFileError con el manual", () => {
  it("acepta un .pdf de hasta 10 MB", () => {
    expect(resourceFileError(file("Manual.PDF", 10 * MB), MANUAL_RULES)).toBeNull();
  });

  it("rechaza otras extensiones, archivos vacíos y archivos de más de 10 MB", () => {
    expect(resourceFileError(file("manual.docx", 10), MANUAL_RULES)).toBe("El manual debe ser un archivo .pdf.");
    expect(resourceFileError(file("manual.pdf", 0), MANUAL_RULES)).toBe("El manual está vacío.");
    expect(resourceFileError(file("manual.pdf", 10 * MB + 1), MANUAL_RULES)).toBe(
      "El manual supera el tamaño máximo permitido de 10 MB.",
    );
    expect(resourceFileError(null, MANUAL_RULES)).toBe("Seleccione el manual.");
  });
});

describe("formatFileSize", () => {
  it("muestra KB por debajo de 1 MB y MB desde 1 MB", () => {
    expect(formatFileSize(2048)).toBe("2.0 KB");
    expect(formatFileSize(1572864)).toBe("1.50 MB");
    expect(formatFileSize(undefined)).toBe("0.0 KB");
  });
});
