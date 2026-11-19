/**
 * Tests for myEventsApi.js (MASON-9 "View personal calendar").
 * fetch() is replaced with a fake, so no backend is needed. Each test lines
 * up the fake answers (one per page of GET /users/me/events/registered).
 */
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { getMyRegisteredEventsInRange } from "./myEventsApi";

function fakeResponse(status, body) {
  return new Response(JSON.stringify(body), { status });
}

function page(items, totalCount, pageNumber = 1) {
  return { items, page: pageNumber, pageSize: 50, totalCount };
}

describe("getMyRegisteredEventsInRange", () => {
  let fetchMock;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("asks the backend for MY registered events (the user comes from the token)", async () => {
    fetchMock.mockResolvedValueOnce(fakeResponse(200, page([{ id: 3, date: "2026-11-20" }], 1)));

    await getMyRegisteredEventsInRange("2026-11-01", "2026-11-30");

    expect(fetchMock.mock.calls[0][0]).toBe("/api/users/me/events/registered?page=1&pageSize=50");
  });

  it("keeps only the events inside the dates on screen (both ends included)", async () => {
    const mine = [
      { id: 1, date: "2026-10-31" }, // day before the range
      { id: 2, date: "2026-11-01" }, // first day
      { id: 3, date: "2026-11-15" },
      { id: 4, date: "2026-11-30" }, // last day
      { id: 5, date: "2026-12-01" }, // day after
    ];
    fetchMock
      .mockResolvedValueOnce(fakeResponse(200, page(mine, 5)));

    const events = await getMyRegisteredEventsInRange("2026-11-01", "2026-11-30");

    expect(events.map((e) => e.id)).toEqual([2, 3, 4]);
  });

  it("loads every page when I registered for more than 50 events", async () => {
    const make = (count, startId) =>
      Array.from({ length: count }, (_, i) => ({ id: startId + i, date: "2026-11-10" }));
    fetchMock
      .mockResolvedValueOnce(fakeResponse(200, page(make(50, 1), 55, 1)))
      .mockResolvedValueOnce(fakeResponse(200, page(make(5, 51), 55, 2)));

    const events = await getMyRegisteredEventsInRange("2026-11-01", "2026-11-30");

    expect(events).toHaveLength(55);
    expect(fetchMock.mock.calls[1][0]).toContain("page=2");
  });

  it("returns an empty list when I haven't registered for anything", async () => {
    fetchMock
      .mockResolvedValueOnce(fakeResponse(200, page([], 0)));

    const events = await getMyRegisteredEventsInRange("2026-11-01", "2026-11-30");

    expect(events).toEqual([]);
    expect(fetchMock).toHaveBeenCalledTimes(1); // no endless page loop
  });

  it("throws with status 401 when nobody is logged in", async () => {
    fetchMock.mockResolvedValueOnce(
      fakeResponse(401, { error: "Unauthorized", code: "UNAUTHORIZED", detail: "Log in first" })
    );

    await expect(getMyRegisteredEventsInRange("2026-11-01", "2026-11-30"))
      .rejects.toMatchObject({ status: 401 });
  });
});