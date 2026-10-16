import { afterEach, describe, expect, it, vi } from "vitest";
import { applyServerTime, getClockOffsetMs, nowMs, resetClockOffset } from "./sessionClock.js";

describe("sessionClock", () => {
  afterEach(() => {
    resetClockOffset();
    vi.useRealTimers();
  });

  it("corrige un reloj local adelantado (SES-05)", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-15T12:20:00.000Z"));

    // Servidor 20 minutos atrás respecto al reloj local.
    applyServerTime(Date.parse("2026-10-15T12:00:00.000Z"));

    expect(getClockOffsetMs()).toBe(-20 * 60_000);
    expect(nowMs()).toBe(Date.parse("2026-10-15T12:00:00.000Z"));
  });

  it("corrige un reloj local atrasado (SES-05)", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-15T11:58:00.000Z"));

    // Servidor 2 minutos adelante respecto al reloj local.
    applyServerTime(Date.parse("2026-10-15T12:00:00.000Z"));

    expect(getClockOffsetMs()).toBe(2 * 60_000);
    expect(nowMs()).toBe(Date.parse("2026-10-15T12:00:00.000Z"));
  });

  it("ignora serverTime inválido y conserva el desfase previo", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-15T12:00:00.000Z"));

    applyServerTime(1_000_000);
    const offset = getClockOffsetMs();

    applyServerTime(undefined);
    applyServerTime(null);
    applyServerTime(Number.NaN);

    expect(getClockOffsetMs()).toBe(offset);
  });
});
