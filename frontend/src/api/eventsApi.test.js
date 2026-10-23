// Tests for eventsApi.js: the ONE file where the frontend talks to the backend.
// Vitest finds this file automatically because its name ends in ".test.js".
// Run them with:  npm test
//
// Replace the browser's fetch()
// with a fake one that returns whatever answer we choose. That lets us check:
//   1. eventsApi sends the right URL, method, and login token
//   2. eventsApi turns our error format {"error","code","detail"} into an Error
//      the pages can read (err.code, err.status, err.message)

// vi = Vitest's helper for fakes, beforeEach/afterEach = run before/after every test.
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";

// The functions we're testing. "./" = same folder as this file.
import { getEvents, registerForEvent } from "./eventsApi";

// Helper: builds the fake answer fetch() gives back.
// Response is built into Node and the browser: it's what a real fetch() returns.
function fakeResponse(status, body) {
  return new Response(body === null ? "" : JSON.stringify(body), { status });
}

describe("eventsApi", () => {
  // fetchMock = our fake fetch. vi.fn() records every call made to it.
  let fetchMock;

  beforeEach(() => {
    fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock); // every fetch() in eventsApi.js now calls fetchMock
  });

  afterEach(() => {
    vi.unstubAllGlobals(); // put the real fetch back so tests don't affect each other
  });

  it("getEvents asks for the right page and returns the backend's answer", async () => {
    const page = { items: [], page: 2, pageSize: 10, totalCount: 0 };
    fetchMock.mockResolvedValue(fakeResponse(200, page)); // the next fetch() returns this

    const result = await getEvents({ page: 2, pageSize: 10 });

    expect(result).toEqual(page);
    // mock.calls[0][0] = the first argument (the URL) of the first call
    expect(fetchMock.mock.calls[0][0]).toBe("/api/events?page=2&pageSize=10");
  });

  it("registerForEvent sends a POST with the login token", async () => {
    fetchMock.mockResolvedValue(
      fakeResponse(201, { id: 1, eventId: 3, userId: 7, createdAt: "2026-10-22T10:00:00" })
    );

    const registration = await registerForEvent(3);

    expect(registration.eventId).toBe(3);
    const [url, options] = fetchMock.mock.calls[0]; // [URL, { method, headers, ... }]
    expect(url).toBe("/api/events/3/register");
    expect(options.method).toBe("POST");
    // No VITE_DEV_TOKEN is set during tests, so eventsApi uses the default test token.
    expect(options.headers.Authorization).toBe("Bearer student-token");
  });

  it("a full event becomes an error with code EVENT_FULL", async () => {
    fetchMock.mockResolvedValue(
      fakeResponse(409, { error: "This event is full", code: "EVENT_FULL", detail: "" })
    );

    // .rejects = "this Promise must fail"; toMatchObject checks just these fields.
    // detail is "", so the message falls back to the "error" text.
    await expect(registerForEvent(3)).rejects.toMatchObject({
      status: 409,
      code: "EVENT_FULL",
      message: "This event is full",
    });
  });

  it("registering twice becomes an error with code ALREADY_REGISTERED", async () => {
    fetchMock.mockResolvedValue(
      fakeResponse(409, { error: "Already registered", code: "ALREADY_REGISTERED", detail: "" })
    );

    await expect(registerForEvent(3)).rejects.toMatchObject({ code: "ALREADY_REGISTERED" });
  });

  it("shows a friendly message when the backend can't be reached", async () => {
    // A rejected fetch() = the request never got any answer (server down, no network).
    fetchMock.mockRejectedValue(new TypeError("Failed to fetch"));

    await expect(getEvents()).rejects.toThrow("Can't reach the server. Is the backend running?");
  });
});
