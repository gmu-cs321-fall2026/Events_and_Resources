// Tests for the calendar's date math.


import { describe, it, expect } from "vitest";
import {
  parseISODate,
  toISODate,
  addMonths,
  getVisibleRange,
  daysBetween,
  shiftAnchor,
  formatTitle,
  formatTime,
  groupEventsByDate,
} from "./calendarDates";

describe("calendarDates", () => {
  it("parses YYYY-MM-DD as a LOCAL date (not shifted to the day before)", () => {
    const d = parseISODate("2026-11-05");
    expect(d.getFullYear()).toBe(2026);
    expect(d.getMonth()).toBe(10); // November (months start at 0)
    expect(d.getDate()).toBe(5);
    expect(toISODate(d)).toBe("2026-11-05"); // round trip
  });

  it("month view covers whole weeks: November 2026 = Sun Nov 1 to Sat Dec 5", () => {
    const { start, end } = getVisibleRange("month", parseISODate("2026-11-18"));
    expect(toISODate(start)).toBe("2026-11-01");
    expect(toISODate(end)).toBe("2026-12-05");
    expect(daysBetween(start, end)).toHaveLength(35); // 5 rows x 7 days
  });

  it("month view starts in the previous month when the 1st isn't a Sunday", () => {
    const { start, end } = getVisibleRange("month", parseISODate("2026-10-10"));
    expect(toISODate(start)).toBe("2026-09-27");
    expect(toISODate(end)).toBe("2026-10-31");
  });

  it("week view is Sunday to Saturday", () => {
    const { start, end } = getVisibleRange("week", parseISODate("2026-11-05")); // a Thursday
    expect(toISODate(start)).toBe("2026-11-01");
    expect(toISODate(end)).toBe("2026-11-07");
  });

  it("day view is just that day", () => {
    const { start, end } = getVisibleRange("day", parseISODate("2026-11-05"));
    expect(toISODate(start)).toBe("2026-11-05");
    expect(toISODate(end)).toBe("2026-11-05");
  });

  it("next/previous move by a month, week, or day, including across years", () => {
    expect(toISODate(shiftAnchor("month", parseISODate("2026-12-15"), 1))).toBe("2027-01-01");
    expect(toISODate(shiftAnchor("week", parseISODate("2026-11-05"), -1))).toBe("2026-10-29");
    expect(toISODate(shiftAnchor("day", parseISODate("2026-10-31"), 1))).toBe("2026-11-01");
  });

  it("adding a month to Jan 31 gives Feb 1, not March", () => {
    expect(toISODate(addMonths(parseISODate("2027-01-31"), 1))).toBe("2027-02-01");
  });

  it("builds readable titles", () => {
    expect(formatTitle("month", parseISODate("2026-11-05"))).toBe("November 2026");
    expect(formatTitle("week", parseISODate("2026-11-05"))).toBe("Nov 1 – Nov 7, 2026");
  });

  it("formats 24-hour times as 12-hour", () => {
    expect(formatTime("00:30")).toBe("12:30 AM");
    expect(formatTime("12:00")).toBe("12:00 PM");
    expect(formatTime("17:30")).toBe("5:30 PM");
    expect(formatTime("oops")).toBe("oops"); // bad data shown as-is, no crash
  });

  it("groups events by day and sorts each day by time", () => {
    const events = [
      { id: 1, date: "2026-11-05", time: "15:00" },
      { id: 2, date: "2026-11-06", time: "10:00" },
      { id: 3, date: "2026-11-05", time: "09:00" },
    ];
    const byDate = groupEventsByDate(events);
    expect(byDate["2026-11-05"].map((e) => e.id)).toEqual([3, 1]);
    expect(byDate["2026-11-06"].map((e) => e.id)).toEqual([2]);
  });
});
