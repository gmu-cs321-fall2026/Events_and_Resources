/**
 * Tests for pageFromHash (MASON-50): the URL's "#" part -> which page to show.
 * Run with: npm test
 */
import { describe, it, expect } from "vitest";
import { PAGES, pageFromHash } from "./hashRoute";

describe("pageFromHash", () => {
	it("finds each page from its hash", () => {
		expect(pageFromHash("#home")).toBe("home");
		expect(pageFromHash("#resources")).toBe("resources");
		expect(pageFromHash("#events")).toBe("events");
		expect(pageFromHash("#jobs")).toBe("jobs");
	});

	it("goes to the home page when there is no hash", () => {
		expect(pageFromHash("")).toBe(PAGES[0].id);
		expect(pageFromHash("#")).toBe(PAGES[0].id);
		expect(pageFromHash(undefined)).toBe(PAGES[0].id);
	});

	it("goes to the home page for an unknown hash instead of a blank screen", () => {
		expect(pageFromHash("#oops")).toBe("home");
	});

	it("sends the old #calendar link to the Events page", () => {
		expect(pageFromHash("#calendar")).toBe("events");
	});

	it("ignores upper/lower case and a leading slash", () => {
		expect(pageFromHash("#Jobs")).toBe("jobs");
		expect(pageFromHash("#/events")).toBe("events");
	});
});