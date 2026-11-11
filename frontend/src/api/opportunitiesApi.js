/**
 * opportunitiesApi.js (MASON-13 Post jobs): the ONE place the frontend talks to
 * the backend about job and internship postings.
 *
 *   getOpportunities({ q, type, page, pageSize })  GET    /opportunities      -> { items, page, pageSize, totalCount }
 *   getOpportunity(id)                             GET    /opportunities/{id} -> one posting
 *   createOpportunity(posting)                     POST   /opportunities      -> the saved posting (staff)
 *   updateOpportunity(id, posting)                 PUT    /opportunities/{id} -> the updated posting (staff)
 *   deleteOpportunity(id)                          DELETE /opportunities/{id} -> nothing (staff)
 *
 * A posting: { id, title, employer, type, description, link, deadline, datePosted }
 * Errors are thrown as an Error with .status and .code from our {"error","code","detail"} format.
 */

const BASE_URL = import.meta.env.PROD ? "" : "/api";
const AUTH_TOKEN = import.meta.env.VITE_DEV_TOKEN || "student-token";

/** The allowed job types. Same list as OpportunityService.TYPES on the backend. */
export const JOB_TYPES = ["Internship", "Full-time", "On-campus"];

/**
 * True when the page runs with a staff or admin test token
 * (VITE_DEV_TOKEN=staff-token npm run dev). Only decides which buttons to SHOW:
 * the backend still checks the role on every request, so hiding a button is
 * never the only protection.
 */
export const IS_STAFF = ["staff-token", "admin-token"].includes(AUTH_TOKEN);

/**
 * Sends one request and returns the JSON body (or null for an empty body, like DELETE's 204).
 * The body is read as text first, so an empty or non-JSON answer doesn't crash.
 */
async function request(path, options = {}) {
	let response;
	try {
		response = await fetch(BASE_URL + path, {
			...options,
			headers: { ...options.headers, Authorization: `Bearer ${AUTH_TOKEN}` },
		});
	} catch {
		throw new Error("Can't reach the server. Is the backend running?");
	}

	const text = await response.text();
	let body = null;
	try {
		body = text ? JSON.parse(text) : null;
	} catch {
		body = null;
	}

	if (!response.ok) {
		const message =
			body?.detail ||
			body?.error ||
			(response.status >= 500 ? "Something went wrong on our side. Please try again." : `Request failed (${response.status})`);
		const error = new Error(message);
		error.status = response.status;
		error.code = body?.code;
		throw error;
	}
	return body;
}

/** Sends a posting as JSON with the given method (POST or PUT). */
function sendPosting(path, method, posting) {
	return request(path, {
		method,
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify(posting),
	});
}

export function getOpportunities({ q = "", type = "", page = 1, pageSize = 20 } = {}) {
	const params = new URLSearchParams({ page, pageSize });
	if (q.trim()) params.set("q", q.trim());
	if (type) params.set("type", type);
	return request(`/opportunities?${params}`);
}

export function getOpportunity(id) {
	return request(`/opportunities/${encodeURIComponent(id)}`);
}

export function createOpportunity(posting) {
	return sendPosting("/opportunities", "POST", posting);
}

export function updateOpportunity(id, posting) {
	return sendPosting(`/opportunities/${encodeURIComponent(id)}`, "PUT", posting);
}

export function deleteOpportunity(id) {
	return request(`/opportunities/${encodeURIComponent(id)}`, { method: "DELETE" });
}

/**
 * "2026-12-15" -> "Dec 15, 2026". Adding "T00:00" makes JavaScript read the date as
 * local midnight, so it doesn't show the day before in Virginia.
 */
export function formatDeadline(date) {
	const d = new Date(`${date}T00:00`);
	if (isNaN(d)) return date;
	return d.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}