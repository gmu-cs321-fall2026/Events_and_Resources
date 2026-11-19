/**
 * auth.js: the ONE place the website keeps the login token.
 *
 * Every API file (eventsApi, resourcesApi, opportunitiesApi) asks this file for the
 * "Authorization" header instead of keeping its own token. Logging in or out here
 * changes it for all of them at once.
 *
 *   login(email, password)  POST /session/login, then GET /session/me -> { userId, roles, staff }
 *   getMe()                 GET  /session/me with the current token   -> { userId, roles, staff }
 *   logout()                forget the token
 *   authHeaders()           { Authorization: "Bearer <token>" }, or {} when logged out
 *   handleUnauthorized()    the API files call this on a 401: the token expired
 *   isStaff(user)           true for staff or admin: decides which buttons to SHOW
 *
 * WHERE THE TOKEN LIVES: in a normal variable (memory), not localStorage. Any script on
 * the page can read localStorage, so a stolen token would keep working. The cost:
 * refreshing the page logs you out.
 *
 * VITE_DEV_TOKEN (optional, fake mode only): start already logged in as a test user,
 *   VITE_DEV_TOKEN=staff-token npm run dev
 * With nothing set, you start logged out and use the login form.
 */

const BASE_URL = import.meta.env.PROD ? "" : "/api";

let token = import.meta.env.VITE_DEV_TOKEN || null;

/** Set by AuthProvider: called when a token stops working, so the page can show the login form. */
let onExpired = null;

export function getToken() {
	return token;
}

/** Replaces the token. login() uses it; tests use it to act as a test user. */
export function setToken(newToken) {
	token = newToken || null;
}

export function authHeaders() {
	return token ? { Authorization: `Bearer ${token}` } : {};
}

export function setOnExpired(callback) {
	onExpired = callback;
}

/**
 * Called by the API files when the backend answers 401.
 * Only counts as "expired" if we HAD a token: a guest who clicks Register gets
 * a 401 too, and shouldn't be told their login expired.
 */
export function handleUnauthorized() {
	if (!token) return;
	token = null;
	if (onExpired) onExpired();
}

export function isStaff(user) {
	return Boolean(user && (user.roles.includes("staff") || user.roles.includes("admin")));
}

/**
 * Sends one request to /session/... and returns the JSON body.
 * Errors become an Error with .status and .code from our {"error","code","detail"} format.
 */
async function sessionRequest(path, options = {}) {
	let response;
	try {
		response = await fetch(BASE_URL + path, options);
	} catch {
		throw new Error("Can't reach the server. Is the backend running?");
	}
	const body = await response.json().catch(() => null);
	if (!response.ok) {
		const error = new Error(body?.error || `Request failed (${response.status})`);
		error.status = response.status;
		error.code = body?.code;
		throw error;
	}
	return body;
}

/**
 * Logs in: email + password -> token, then asks who that token belongs to.
 * The token is only kept if both steps work.
 */
export async function login(email, password) {
	const { token: newToken } = await sessionRequest("/session/login", {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ email, password }),
	});
	const me = await sessionRequest("/session/me", {
		headers: { Authorization: `Bearer ${newToken}` },
	});
	token = newToken;
	return me;
}

/** Who is logged in with the current token? Throws (401) if nobody. */
export function getMe() {
	return sessionRequest("/session/me", { headers: authHeaders() });
}

export function logout() {
	token = null;
}
