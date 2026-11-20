/**
 * resourcesApi.js: the ONE place the website talks to the resources endpoints.
 *
 * Browse (MASON-12, anyone)
 *   getResources({ q, major, type })   GET /resources -> approved resources only
 *   getUpcomingEvents(major)           GET /events    -> the next few events for that major
 *
 * Share (MASON-7, logged in)
 *   submitResource(resource)           POST   /resources       -> starts "pending" (staff: "approved")
 *   updateResource(id, resource)       PUT    /resources/{id}  -> only while pending
 *   deleteResource(id)                 DELETE /resources/{id}  -> only while pending
 *   getMyResources()                   GET    /users/me/resources -> my submissions, every status
 *
 * Review (MASON-11, staff/admin)
 *   getReviewQueue(status)             GET /resources?status=pending|approved|rejected
 *   reviewResource(id, status, note)   PUT /resources/{id}/status
 *
 * A resource (the shape the cards and panels use):
 *   { id, title, description, type, industry, link, majors: [..], sharedBy, status,
 *     reviewNote, submittedAt, reviewedAt }
 */

import { authHeaders, handleUnauthorized } from "./auth";

const BASE_URL = import.meta.env.PROD ? "" : "/api";

export const MAJORS = [
  "All majors",
  "CS Majors",
  "IT Majors",
  "Freshmen",
  "Sophomores",
  "Juniors",
  "Seniors",
  "Graduate Students",
];

export const RESOURCE_TYPES = [
  "All types",
  "Resume & Cover Letter",
  "Interview Questions",
  "Salary Negotiation",
  "Elevator Pitch",
  "Application Tips",
  "Practice Tools",
];

/** Audiences a resource can be shared with (the server's audiences table). */
export const AUDIENCES = ["All Students", ...MAJORS.slice(1)];

/** Types a resource can have (RESOURCE_TYPES without "All types"). */
export const SHAREABLE_TYPES = RESOURCE_TYPES.slice(1);

/** How each status is shown to people. "rejected" is called "Denied" on screen. */
export const STATUS_LABELS = {
  pending: "Pending review",
  approved: "Posted",
  rejected: "Denied",
};

/**
 * Small fetch helper for this file (eventsApi.js keeps its own; we don't edit it).
 * options = { method, body }. A body object is sent as JSON.
 * Returns the JSON answer, or null for "204 No Content".
 * Throws an Error with .status, .code and .detail from our {"error","code","detail"} format.
 */
async function request(path, { method = "GET", body } = {}) {
  const headers = { ...authHeaders() };
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response;
  try {
    response = await fetch(BASE_URL + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new Error("Can't reach the server. Is the backend running?");
  }
  if (response.status === 204) return null;

  const data = await response.json().catch(() => null);
  if (!response.ok) {
    if (response.status === 401) handleUnauthorized();
    const message = data?.detail ? `${data.error}: ${data.detail}` : data?.error;
    const error = new Error(message || `Request failed (${response.status})`);
    error.status = response.status;
    error.code = data?.code;
    error.detail = data?.detail;
    throw error;
  }
  return data;
}

/**
 * The matching rules (the same ones GET /resources will use on the server):
 *   major: "All majors", or the resource lists that major, or it's for "All Students"
 *   type:  "All types", or an exact match
 *   q:     found in the title, description, industry or type (upper/lower case ignored)
 *   only status "approved"
 * Exported so the test can check the rules directly.
 */
export function matchesFilters(resource, { q = "", major = "All majors", type = "All types" } = {}) {
  const text = `${resource.title} ${resource.description} ${resource.industry} ${resource.type}`.toLowerCase();
  const majorOk = major === "All majors" || resource.majors.includes(major) || resource.majors.includes("All Students");
  const typeOk = type === "All types" || resource.type === type;
  const queryOk = q.trim() === "" || text.includes(q.trim().toLowerCase());
  return resource.status === "approved" && majorOk && typeOk && queryOk;
}

/**
 * Turns one resource from the server into the shape the cards use.
 * link stays null when there is none, so the card can hide its "Open" button.
 */
export function toCard(r) {
  return {
    id: r.id,
    title: r.title,
    description: r.description ?? "",
    type: r.type ?? "",
    industry: r.industry ?? "All industries",
    link: r.link || null,
    majors: r.majors?.length ? r.majors : ["All Students"],
    sharedBy: r.sharedBy ?? "Career Services",
    status: r.status ?? "approved",
    reviewNote: r.reviewNote ?? null,
    submittedAt: r.submittedAt ?? null,
    reviewedAt: r.reviewedAt ?? null,
  };
}

/** The directory: approved resources that match the filters (the server applies matchesFilters' rules). */
export async function getResources(filters = {}) {
  const params = new URLSearchParams();
  if (filters.q) params.set("q", filters.q);
  if (filters.major && filters.major !== "All majors") params.set("major", filters.major);
  if (filters.type && filters.type !== "All types") params.set("type", filters.type);
  params.set("pageSize", "50");
  const page = await request(`/resources?${params}`);
  return page.items.map(toCard);
}

/**
 * The body POST and PUT send. Only these fields: the server decides status and sharedBy.
 * An empty link is sent as null ("no link").
 */
function toRequestBody(resource) {
  return {
    title: resource.title.trim(),
    description: resource.description.trim(),
    type: resource.type,
    industry: resource.industry.trim(),
    link: resource.link.trim() || null,
    majors: resource.majors,
  };
}

/** MASON-7: share a resource. Students' start as "pending"; staff/admin posts are "approved". */
export async function submitResource(resource) {
  return toCard(await request("/resources", { method: "POST", body: toRequestBody(resource) }));
}

/** MASON-7: edit my resource. The server answers 409 NOT_PENDING once it's been reviewed. */
export async function updateResource(id, resource) {
  return toCard(await request(`/resources/${id}`, { method: "PUT", body: toRequestBody(resource) }));
}

/** MASON-7: delete my resource (only while pending). */
export async function deleteResource(id) {
  await request(`/resources/${id}`, { method: "DELETE" });
}

/** MASON-7: everything I shared, with its status, newest first. */
export async function getMyResources() {
  const page = await request("/users/me/resources?pageSize=50");
  return page.items.map(toCard);
}

/** MASON-11: the staff review list for one status ("pending" = waiting, oldest first). */
export async function getReviewQueue(status = "pending") {
  const params = new URLSearchParams({ status, pageSize: "50" });
  const page = await request(`/resources?${params}`);
  return page.items.map(toCard);
}

/**
 * MASON-11: staff decision.
 * status = "approved" (post it), "rejected" (deny it) or "pending" (put it on hold).
 * note   = optional message the student sees.
 */
export async function reviewResource(id, status, note = "") {
  const body = { status, note: note.trim() || null };
  return toCard(await request(`/resources/${id}/status`, { method: "PUT", body }));
}

/** "2026-11-19T10:15:00" -> "Nov 19, 2026". Empty string if there's no date. */
export function formatDate(timestamp) {
  if (!timestamp) return "";
  const [year, month, day] = timestamp.slice(0, 10).split("-").map(Number);
  return new Date(year, month - 1, day).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

/**
 * The next few events for a major, from the real GET /events (read only).
 * from = today, so past events don't show.
 * For one major we ask twice, that major and "All Students" (everyone can go to those),
 * then merge, sort soonest first and keep the first few. Promise.all runs both at once.
 */
export async function getUpcomingEvents(major, howMany = 4) {
  const today = new Date().toISOString().slice(0, 10);
  const audiences = !major || major === "All majors" ? [null] : [major, "All Students"];

  const pages = await Promise.all(
    audiences.map((audience) => {
      const params = new URLSearchParams({ from: today, pageSize: String(howMany) });
      if (audience) params.set("audience", audience);
      return request(`/events?${params}`);
    })
  );

  const byId = new Map();
  pages.flatMap((page) => page.items).forEach((event) => byId.set(event.id, event));
  return [...byId.values()]
    .sort((a, b) => `${a.date} ${a.time}`.localeCompare(`${b.date} ${b.time}`))
    .slice(0, howMany);
}
