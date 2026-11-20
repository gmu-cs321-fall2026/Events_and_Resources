/**
 * Tests for the Events page filter helpers (MASON-10 Search bar, Events tab).
 */
import { describe, expect, it } from "vitest";
import {
  ALL_CATEGORIES,
  ALL_FORMATS,
  describeFilters,
  matchesFilters,
  matchesKeyword,
  toApiFilters,
} from "./eventSearch";

const event = {
  eventTitle: "Resume Review Workshop",
  host: "Career Services",
  location: "Johnson Center 240",
  audience: "CS Majors",
  category: "Workshop",
  format: "In-person",
};

describe("matchesKeyword", () => {
  it("matches every event when the keyword is empty or only spaces", () => {
    expect(matchesKeyword(event, "")).toBe(true);
    expect(matchesKeyword(event, "   ")).toBe(true);
    expect(matchesKeyword(event, undefined)).toBe(true);
  });

  it("ignores upper/lower case", () => {
    expect(matchesKeyword(event, "RESUME")).toBe(true);
  });

  it("looks in title, host, location, audience and category", () => {
    expect(matchesKeyword(event, "review")).toBe(true);
    expect(matchesKeyword(event, "career services")).toBe(true);
    expect(matchesKeyword(event, "johnson")).toBe(true);
    expect(matchesKeyword(event, "cs majors")).toBe(true);
    expect(matchesKeyword(event, "workshop")).toBe(true);
  });

  it("does not look in other fields, same as the backend", () => {
    expect(matchesKeyword(event, "in-person")).toBe(false);
  });

  it("works when audience is a list", () => {
    expect(matchesKeyword({ ...event, audience: ["Juniors", "Seniors"] }, "senior")).toBe(true);
  });

  it("returns false when nothing matches", () => {
    expect(matchesKeyword(event, "capital one")).toBe(false);
  });
});

describe("matchesFilters", () => {
  it("passes every event when no filter is on", () => {
    expect(matchesFilters(event, {})).toBe(true);
  });

  it("checks category and format, ignoring upper/lower case", () => {
    expect(matchesFilters(event, { category: "workshop" })).toBe(true);
    expect(matchesFilters(event, { category: "Career Fair" })).toBe(false);
    expect(matchesFilters(event, { format: "In-person" })).toBe(true);
    expect(matchesFilters(event, { format: "Virtual" })).toBe(false);
  });

  it("needs every filter to pass", () => {
    expect(matchesFilters(event, { q: "resume", category: "Workshop", format: "In-person" })).toBe(true);
    expect(matchesFilters(event, { q: "resume", category: "Workshop", format: "Virtual" })).toBe(false);
  });
});

describe("toApiFilters / describeFilters", () => {
  it('turns the "All ..." choices into empty filters', () => {
    expect(toApiFilters({ keyword: "", category: ALL_CATEGORIES, format: ALL_FORMATS }))
      .toEqual({ q: "", category: "", format: "" });
  });

  it("describes the filters that are on", () => {
    expect(describeFilters({ q: "resume", category: "Workshop", format: "" })).toBe('"resume" · Workshop');
  });
});
