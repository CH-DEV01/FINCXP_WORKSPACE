import { describe, expect, it } from "vitest";
import { holidaySchema } from "./holidaySchema.js";

describe("holidaySchema (VAL-04)", () => {
  it("rejects a description made only of spaces", () => {
    const result = holidaySchema.safeParse({
      holidayDate: "2026-12-25",
      description: "      ",
    });

    expect(result.success).toBe(false);
  });

  it("trims the description before applying the minimum length", () => {
    const result = holidaySchema.safeParse({
      holidayDate: "2026-12-25",
      description: "  Navidad  ",
    });

    expect(result.success).toBe(true);
    expect(result.data.description).toBe("Navidad");
  });
});
