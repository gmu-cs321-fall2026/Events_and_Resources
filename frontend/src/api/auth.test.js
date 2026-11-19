/**
 * Tests for auth.js: logging in, the Authorization header, and expired logins.
 * Like eventsApi.test.js, fetch() is replaced with a fake that answers whatever each test says.
 * Run with: npm test
 */
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
	authHeaders,
	getToken,
	handleUnauthorized,
	isStaff,
	login,
	logout,
	setOnExpired,
	setToken,
} from "./auth";

function fakeResponse(status, body) {
	return new Response(JSON.stringify(body), { status });
}

const STAFF_ME = { userId: "00000000-0000-0000-0000-000000000002", roles: ["staff"], staff: true };

describe("auth", () => {
	let fetchMock;

	beforeEach(() => {
		fetchMock = vi.fn();
		vi.stubGlobal("fetch", fetchMock);
		setToken(null);
	});

	afterEach(() => {
		vi.unstubAllGlobals();
		setToken(null);
		setOnExpired(null);
	});

	it("login gets a token, asks who it belongs to, and keeps the token", async () => {
		fetchMock
			.mockResolvedValueOnce(fakeResponse(200, { token: "abc", expiresIn: 3600 }))
			.mockResolvedValueOnce(fakeResponse(200, STAFF_ME));

		const me = await login("staff@gmu.edu", "ChangeMe123!");

		expect(me).toEqual(STAFF_ME);
		expect(getToken()).toBe("abc");

		const [loginUrl, loginOptions] = fetchMock.mock.calls[0];
		expect(loginUrl).toBe("/api/session/login");
		expect(loginOptions.method).toBe("POST");
		expect(JSON.parse(loginOptions.body)).toEqual({ email: "staff@gmu.edu", password: "ChangeMe123!" });

		const [meUrl, meOptions] = fetchMock.mock.calls[1];
		expect(meUrl).toBe("/api/session/me");
		expect(meOptions.headers.Authorization).toBe("Bearer abc");
	});

	it("a wrong password throws the backend's message and keeps you logged out", async () => {
		fetchMock.mockResolvedValueOnce(
			fakeResponse(401, { error: "Wrong email or password", code: "UNAUTHORIZED", detail: "" })
		);

		await expect(login("staff@gmu.edu", "nope")).rejects.toMatchObject({
			status: 401,
			message: "Wrong email or password",
		});
		expect(getToken()).toBeNull();
	});

	it("authHeaders is empty when logged out and has the token when logged in", () => {
		expect(authHeaders()).toEqual({});
		setToken("staff-token");
		expect(authHeaders()).toEqual({ Authorization: "Bearer staff-token" });
		logout();
		expect(authHeaders()).toEqual({});
	});

	it("handleUnauthorized only reports an expired login if there was a token", () => {
		const expired = vi.fn();
		setOnExpired(expired);

		handleUnauthorized(); // a guest got a 401: nothing expired
		expect(expired).not.toHaveBeenCalled();

		setToken("old-token");
		handleUnauthorized();
		expect(expired).toHaveBeenCalledTimes(1);
		expect(getToken()).toBeNull();
	});

	it("isStaff is true for staff or admin only", () => {
		expect(isStaff(null)).toBe(false);
		expect(isStaff({ roles: ["student", "mentor"] })).toBe(false);
		expect(isStaff({ roles: ["staff"] })).toBe(true);
		expect(isStaff({ roles: ["admin"] })).toBe(true);
	});
});