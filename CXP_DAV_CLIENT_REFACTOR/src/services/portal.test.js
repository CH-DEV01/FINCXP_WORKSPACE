import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import axios from "axios";
import { PORTAL_URL } from "../constants/apiConstants";

vi.mock("axios", () => ({ default: { get: vi.fn() } }));

const loadPortal = async () => {
  vi.resetModules();
  return import("./portal");
};

describe("portalUrl", () => {
  beforeEach(() => axios.get.mockReset());
  afterEach(() => vi.restoreAllMocks());

  it("usa APP_LOGIN_URL que entrega la API", async () => {
    axios.get.mockResolvedValue({ data: { data: { portalUrl: "https://pay.davivienda.com.sv/login" } } });
    const { portalUrl } = await loadPortal();

    await expect(portalUrl()).resolves.toBe("https://pay.davivienda.com.sv/login");
  });

  it("consulta la API una sola vez", async () => {
    axios.get.mockResolvedValue({ data: { data: { portalUrl: "https://pay.davivienda.com.sv" } } });
    const { portalUrl } = await loadPortal();

    await portalUrl();
    await portalUrl();

    expect(axios.get).toHaveBeenCalledTimes(1);
  });

  it("vuelve a PORTAL_URL si el parámetro no está configurado", async () => {
    axios.get.mockResolvedValue({ data: { data: { portalUrl: null } } });
    const { portalUrl } = await loadPortal();

    await expect(portalUrl()).resolves.toBe(PORTAL_URL);
  });

  it("vuelve a PORTAL_URL si el valor no es una URL http(s)", async () => {
    axios.get.mockResolvedValue({ data: { data: { portalUrl: "javascript:alert(1)" } } });
    const { portalUrl } = await loadPortal();

    await expect(portalUrl()).resolves.toBe(PORTAL_URL);
  });

  it("vuelve a PORTAL_URL si la API no responde", async () => {
    axios.get.mockRejectedValue(new Error("Network Error"));
    const { portalUrl } = await loadPortal();

    await expect(portalUrl()).resolves.toBe(PORTAL_URL);
  });
});
