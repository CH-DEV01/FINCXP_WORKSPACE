import { describe, expect, it } from "vitest";
import { matchesHoliday } from "./HolidayManagement.jsx";

describe("matchesHoliday", () => {
  const christmas = { holidayDate: "2026-12-25", description: "Navidad" };

  it("finds a holiday by the displayed DD/MM/YYYY date", () => {
    expect(matchesHoliday(christmas, "25/12")).toBe(true);
    expect(matchesHoliday(christmas, "25/12/2026")).toBe(true);
  });

  it("still finds by ISO date and description", () => {
    expect(matchesHoliday(christmas, "2026-12-25")).toBe(true);
    expect(matchesHoliday(christmas, "navidad")).toBe(true);
  });

  it("rejects unrelated terms", () => {
    expect(matchesHoliday(christmas, "01/01")).toBe(false);
  });
});
