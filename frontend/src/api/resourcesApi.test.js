/**
 * Tests for the Resources page's filter rules (MASON-12 acceptance criteria).
 * matchesFilters is the same rule GET /resources will follow on the server.
 */
import { describe, expect, it } from "vitest";
import { getResources, matchesFilters } from "./resourcesApi";

const csGuide = { title: "Tech resume checklist", description: "One page", industry: "Technology", type: "Resume & Cover Letter", majors: ["CS Majors"], status: "approved" };
const forEveryone = { title: "Negotiating your first offer", description: "Salary tips", industry: "All industries", type: "Salary Negotiation", majors: ["All Students"], status: "approved" };
const pending = { ...csGuide, status: "pending" };

describe("matchesFilters", () => {
  it("shows a major's own resources plus 'All Students' ones", () => {
    expect(matchesFilters(csGuide, { major: "CS Majors" })).toBe(true);
    expect(matchesFilters(forEveryone, { major: "CS Majors" })).toBe(true);
    expect(matchesFilters(csGuide, { major: "IT Majors" })).toBe(false);
  });

  it("searches title, description and industry, ignoring case", () => {
    expect(matchesFilters(csGuide, { q: "RESUME" })).toBe(true);
    expect(matchesFilters(csGuide, { q: "technology" })).toBe(true);
    expect(matchesFilters(csGuide, { q: "finance" })).toBe(false);
  });

  it("filters by type", () => {
    expect(matchesFilters(forEveryone, { type: "Salary Negotiation" })).toBe(true);
    expect(matchesFilters(forEveryone, { type: "Elevator Pitch" })).toBe(false);
  });

  it("never shows resources that aren't approved", () => {
    expect(matchesFilters(pending, {})).toBe(false);
  });
});

describe("getResources (sample data)", () => {
  it("every result shows who shared it", async () => {
    const results = await getResources();
    expect(results.length).toBeGreaterThan(0);
    results.forEach((r) => expect(r.sharedBy).toBeTruthy());
  });
});
