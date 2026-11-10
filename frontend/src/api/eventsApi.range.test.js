// Tests for the calendar's part of eventsApi.js date filters and
// getEventsInRange's "keep asking for the next page" loop.
// Kept in its own file so it doesn't collide with other people's edits to eventsApi.test.js.

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { getEvents, getEventsInRange } from "./eventsApi";

// The fake answer fetch() gives back (same helper as eventsApi.test.js).
function fakeResponse(status, body) {
  return new Response(JSON.stringify(body), { status });
}

describe("eventsApi date range", () => {
  let fetchMock;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock); // replace the real fetch() with our fake
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("getEvents sends from/to filters and skips empty ones", async () => {
    fetchMock.mockResolvedValue(fakeResponse(200, { items: [], page: 1, pageSize: 50, totalCount: 0 }));

    await getEvents({ page: 1, pageSize: 50, from: "2026-11-01", to: "2026-12-05", category: "" });

    // category was "" so it must NOT appear in the URL.
    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/events?page=1&pageSize=50&from=2026-11-01&to=2026-12-05"
    );
  });

  it("getEventsInRange keeps fetching pages until it has every event", async () => {
    // 60 matching events: page 1 has 50, page 2 has 10.
    const makeEvents = (count, startId) =>
      Array.from({ length: count }, (_, i) => ({ id: startId + i, date: "2026-11-05" }));
    fetchMock
      .mockResolvedValueOnce(fakeResponse(200, { items: makeEvents(50, 1), page: 1, pageSize: 50, totalCount: 60 }))
      .mockResolvedValueOnce(fakeResponse(200, { items: makeEvents(10, 51), page: 2, pageSize: 50, totalCount: 60 }));

    const events = await getEventsInRange("2026-11-01", "2026-12-05");

    expect(events).toHaveLength(60);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[1][0]).toContain("page=2");
  });

  it("getEventsInRange makes one request when everything fits on one page", async () => {
    fetchMock.mockResolvedValue(
      fakeResponse(200, { items: [{ id: 1, date: "2026-11-05" }], page: 1, pageSize: 50, totalCount: 1 })
    );

    const events = await getEventsInRange("2026-11-01", "2026-11-30");

    expect(events).toHaveLength(1);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
