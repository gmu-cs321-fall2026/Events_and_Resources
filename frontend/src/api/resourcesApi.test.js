/**
 * Tests for the Resources page's filter rules (MASON-12 acceptance criteria).
 * matchesFilters is the same rule GET /resources follows on the server.
 * The API calls below replace fetch() with a fake, like opportunitiesApi.test.js.
 */
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  deleteResource,
  formatDate,
  getMyResources,
  getResources,
  getReviewQueue,
  matchesFilters,
  reviewResource,
  submitResource,
  updateResource,
} from "./resourcesApi";

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

function fakeResponse(status, body) {
  return new Response(body === null ? null : JSON.stringify(body), { status });
}

const FROM_SERVER = {
  id: 7, title: "Fintech fair questions", description: "Five questions", type: "Interview Questions",
  industry: "Finance", link: null, majors: ["All Students"], sharedBy: "Student submission",
  status: "pending", userId: "00000000-0000-0000-0000-000000000003", reviewNote: null,
  submittedAt: "2026-11-19T10:15:00", reviewedAt: null,
};
const page = (items) => ({ items, page: 1, pageSize: 50, totalCount: items.length });

const FORM = {
  title: "  Fintech fair questions ", description: "Five questions", type: "Interview Questions",
  industry: " Finance ", link: "  ", majors: ["All Students"],
};

describe("resources API calls (MASON-7 / MASON-11)", () => {
  let fetchMock;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("getResources asks the server with the filters and every result shows who shared it", async () => {
    fetchMock.mockResolvedValue(fakeResponse(200, page([{ ...FROM_SERVER, status: "approved" }])));

    const results = await getResources({ q: "fintech", major: "CS Majors", type: "All types" });

    const url = fetchMock.mock.calls[0][0];
    expect(url).toContain("/resources?");
    expect(url).toContain("q=fintech");
    expect(url).toContain("major=CS+Majors");
    expect(url).not.toContain("type=");
    results.forEach((r) => expect(r.sharedBy).toBeTruthy());
  });

  it("submitResource POSTs only the editable fields, trimmed, with an empty link as null", async () => {
    fetchMock.mockResolvedValue(fakeResponse(201, FROM_SERVER));

    const saved = await submitResource(FORM);

    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toMatch(/\/resources$/);
    expect(options.method).toBe("POST");
    expect(JSON.parse(options.body)).toEqual({
      title: "Fintech fair questions", description: "Five questions", type: "Interview Questions",
      industry: "Finance", link: null, majors: ["All Students"],
    });
    expect(saved.status).toBe("pending");
  });

  it("updateResource and deleteResource use the resource's id", async () => {
    fetchMock.mockResolvedValueOnce(fakeResponse(200, FROM_SERVER)).mockResolvedValueOnce(fakeResponse(204, null));

    await updateResource(7, FORM);
    await deleteResource(7);

    expect(fetchMock.mock.calls[0][0]).toMatch(/\/resources\/7$/);
    expect(fetchMock.mock.calls[0][1].method).toBe("PUT");
    expect(fetchMock.mock.calls[1][1].method).toBe("DELETE");
  });

  it("a reviewed resource can't be edited: the 409 NOT_PENDING code reaches the form", async () => {
    fetchMock.mockResolvedValue(fakeResponse(409, { error: "This resource is already approved", code: "NOT_PENDING", detail: "" }));

    await expect(updateResource(7, FORM)).rejects.toMatchObject({ status: 409, code: "NOT_PENDING" });
  });

  it("getMyResources reads /users/me/resources and keeps the status and note", async () => {
    fetchMock.mockResolvedValue(fakeResponse(200, page([{ ...FROM_SERVER, status: "rejected", reviewNote: "Broken link" }])));

    const [mine] = await getMyResources();

    expect(fetchMock.mock.calls[0][0]).toContain("/users/me/resources");
    expect(mine.status).toBe("rejected");
    expect(mine.reviewNote).toBe("Broken link");
  });

  it("staff review: getReviewQueue asks for a status, reviewResource PUTs the decision", async () => {
    fetchMock.mockResolvedValueOnce(fakeResponse(200, page([FROM_SERVER])))
      .mockResolvedValueOnce(fakeResponse(200, { ...FROM_SERVER, status: "approved" }));

    await getReviewQueue("pending");
    const decided = await reviewResource(7, "approved", "  ");

    expect(fetchMock.mock.calls[0][0]).toContain("status=pending");
    const [url, options] = fetchMock.mock.calls[1];
    expect(url).toMatch(/\/resources\/7\/status$/);
    expect(JSON.parse(options.body)).toEqual({ status: "approved", note: null });
    expect(decided.status).toBe("approved");
  });

  it("validation errors show the server's detail", async () => {
    fetchMock.mockResolvedValue(fakeResponse(400, { error: "Some fields are invalid", code: "VALIDATION_ERROR", detail: "title: required" }));

    await expect(submitResource(FORM)).rejects.toThrow("Some fields are invalid: title: required");
  });
});

describe("formatDate", () => {
  it("shows a server timestamp as a short date", () => {
    expect(formatDate("2026-11-19T10:15:00")).toBe("Nov 19, 2026");
    expect(formatDate(null)).toBe("");
  });
});
