import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("sweetalert2", () => ({
  default: { fire: vi.fn(() => Promise.resolve({})), showLoading: vi.fn() },
}));

import Swal from "sweetalert2";
import { apiErrorMessage, readApiErrorMessage, showApiError, showError, showLoading, showSuccess } from "./errors.js";

const httpError = (data) => ({ isAxiosError: true, message: "Request failed", response: { data } });

describe("apiErrorMessage", () => {
  it("usa el mensaje del ApiResponse del backend", () => {
    expect(apiErrorMessage(httpError({ message: "Saldo insuficiente" }), "Falló")).toBe("Saldo insuficiente");
  });

  it("usa el respaldo si la respuesta no trae mensaje", () => {
    expect(apiErrorMessage(httpError({}), "Falló")).toBe("Falló");
    expect(apiErrorMessage(httpError(undefined), "Falló")).toBe("Falló");
  });

  it("usa el mensaje de un Error propio (no axios)", () => {
    expect(apiErrorMessage(new Error("Seleccione un archivo"), "Falló")).toBe("Seleccione un archivo");
  });

  it("no expone el mensaje técnico de axios sin respuesta (red caída)", () => {
    expect(apiErrorMessage({ isAxiosError: true, message: "Network Error" }, "Falló")).toBe("Falló");
  });

  it("usa el respaldo con errores nulos", () => {
    expect(apiErrorMessage(null, "Falló")).toBe("Falló");
    expect(apiErrorMessage(undefined, "Falló")).toBe("Falló");
  });
});

describe("readApiErrorMessage", () => {
  it("lee el mensaje de un cuerpo Blob con JSON", async () => {
    const blob = new Blob([JSON.stringify({ message: "Carta no disponible" })], {
      type: "application/json",
    });
    await expect(readApiErrorMessage(httpError(blob), "Falló")).resolves.toBe("Carta no disponible");
  });

  it("usa el respaldo si el Blob no es JSON", async () => {
    const blob = new Blob(["<html>error</html>"], { type: "text/html" });
    await expect(readApiErrorMessage(httpError(blob), "Falló")).resolves.toBe("Falló");
  });

  it("usa el respaldo si el JSON del Blob no trae mensaje", async () => {
    const blob = new Blob([JSON.stringify({ status: 500 })]);
    await expect(readApiErrorMessage(httpError(blob), "Falló")).resolves.toBe("Falló");
  });

  it("delega en apiErrorMessage para cuerpos que no son Blob", async () => {
    await expect(readApiErrorMessage(httpError({ message: "Sin permisos" }), "Falló")).resolves.toBe("Sin permisos");
    await expect(readApiErrorMessage(new Error("Local"), "Falló")).resolves.toBe("Local");
  });
});

describe("alertas", () => {
  beforeEach(() => {
    Swal.fire.mockClear();
    Swal.showLoading.mockClear();
  });

  it("showError muestra un ícono de error con el texto", () => {
    showError("Algo salió mal");
    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ icon: "error", title: "Error", text: "Algo salió mal" }),
    );
  });

  it("showSuccess se cierra solo", () => {
    showSuccess("Guardado", "Listo");
    expect(Swal.fire).toHaveBeenCalledWith(
      expect.objectContaining({ icon: "success", title: "Listo", text: "Guardado", timer: 2000 }),
    );
  });

  it("showApiError muestra el mensaje del backend", () => {
    showApiError(httpError({ message: "Duplicado" }), "Falló", "No se pudo guardar");
    expect(Swal.fire).toHaveBeenCalledWith(expect.objectContaining({ title: "No se pudo guardar", text: "Duplicado" }));
  });

  it("showLoading bloquea el cierre y activa el indicador al abrir", () => {
    showLoading();
    const options = Swal.fire.mock.calls[0][0];
    expect(options).toMatchObject({ title: "Procesando...", allowOutsideClick: false });
    options.didOpen();
    expect(Swal.showLoading).toHaveBeenCalled();
  });
});
