import { describe, expect, it } from "vitest";
import { shouldClearSessionOnProfileError } from "./sessionProfileError.js";

describe("shouldClearSessionOnProfileError (SES-06)", () => {
  it("borra la sesión local solo ante un 401", () => {
    expect(shouldClearSessionOnProfileError({ response: { status: 401 } })).toBe(true);
  });

  it("conserva la sesión ante 503, 500, timeout o error de red", () => {
    expect(shouldClearSessionOnProfileError({ response: { status: 503 } })).toBe(false);
    expect(shouldClearSessionOnProfileError({ response: { status: 500 } })).toBe(false);
    expect(shouldClearSessionOnProfileError({ code: "ECONNABORTED" })).toBe(false);
    expect(shouldClearSessionOnProfileError({})).toBe(false);
    expect(shouldClearSessionOnProfileError(undefined)).toBe(false);
  });
});
