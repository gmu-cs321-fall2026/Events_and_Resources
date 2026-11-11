/**
 * Tests for opportunitiesApi.js (MASON-13). Same idea as eventsApi.test.js:
 * fetch() is replaced by a fake, so we can check the URL, method and body we send,
 * and how errors from our {"error","code","detail"} format are turned into an Error.
 * Run with: npm test
 */
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
	createOpportunity,
	deleteOpportunity,
	formatDeadline,
	getOpportunities,
	updateOpportunity,
} from "./opportunitiesApi";

function fakeResponse(status, body) {
	return new Response(body === null ? null : JSON.stringify(body), { status });
}

const POSTING = {
	title: "Software Intern",
	employer: "Capital One",
	type: "Internship",
	deadline: "2026-12-15",
	link: "https://example.com/apply",
	description: "Build features with a mentor.",
};

describe("opportunitiesApi", () => {
	let fetchMock;

	beforeEach(() => {
		fetchMock = vi.fn();
		vi.stubGlobal("fetch", fetchMock);
	});

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it("getOpportunities sends the search and type, and leaves out empty filters", async () => {
		// A new answer for every call: a Response's body can only be read once.
		fetchMock.mockImplementation(async () => fakeResponse(200, { items: [], page: 1, pageSize: 20, totalCount: 0 }));

		await getOpportunities({ q: " intern ", type: "Internship" });
		await getOpportunities();

		expect(fetchMock.mock.calls[0][0]).toBe("/api/opportunities?page=1&pageSize=20&q=intern&type=Internship");
		expect(fetchMock.mock.calls[1][0]).toBe("/api/opportunities?page=1&pageSize=20");
	});

	it("createOpportunity POSTs the posting as JSON", async () => {
		fetchMock.mockResolvedValue(fakeResponse(201, { id: 9, ...POSTING, datePosted: "2026-11-11" }));

		const saved = await createOpportunity(POSTING);

		const [url, options] = fetchMock.mock.calls[0];
		expect(url).toBe("/api/opportunities");
		expect(options.method).toBe("POST");
		expect(JSON.parse(options.body)).toEqual(POSTING);
		expect(saved.id).toBe(9);
	});

	it("updateOpportunity PUTs to the posting's own URL", async () => {
		fetchMock.mockResolvedValue(fakeResponse(200, { id: 9, ...POSTING }));

		await updateOpportunity(9, POSTING);

		expect(fetchMock.mock.calls[0][0]).toBe("/api/opportunities/9");
		expect(fetchMock.mock.calls[0][1].method).toBe("PUT");
	});

	it("deleteOpportunity handles the empty 204 answer", async () => {
		fetchMock.mockResolvedValue(fakeResponse(204, null));

		await expect(deleteOpportunity(9)).resolves.toBeNull();
		expect(fetchMock.mock.calls[0][1].method).toBe("DELETE");
	});

	it("a 403 from the backend becomes an Error with its code", async () => {
		fetchMock.mockResolvedValue(
			fakeResponse(403, { error: "Only Career Services staff or admin can do this", code: "FORBIDDEN", detail: "" })
		);

		const err = await createOpportunity(POSTING).catch((e) => e);

		expect(err.status).toBe(403);
		expect(err.code).toBe("FORBIDDEN");
		expect(err.message).toBe("Only Career Services staff or admin can do this");
	});

	it("formatDeadline shows a short date and keeps bad input as it is", () => {
		expect(formatDeadline("2026-12-15")).toMatch(/Dec.*15.*2026/);
		expect(formatDeadline("not a date")).toBe("not a date");
	});
});