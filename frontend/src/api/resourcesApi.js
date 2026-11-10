/**
 * resourcesApi.js: the ONE place the Resources page gets its data.
 *
 * getResources({ q, major, type })  -> resources that match (only "approved" ones)
 * getUpcomingEvents(major)           -> the next few events for that major (real GET /events)
 *
 * RESOURCES USE SAMPLE DATA FOR NOW.
 * Jean's MASON-7 (submit a resource) owns the resource table. Once her columns are on
 * main and GET /resources exists, set USE_SAMPLE_RESOURCES to false: the page doesn't change.
 * If her field names differ, adjust them in toCard() below, nowhere else.
 *
 * A resource (the basic shape the cards use):
 *   { id, title, description, type, industry, link, majors: [..], sharedBy, status }
 */

const USE_SAMPLE_RESOURCES = true;

const BASE_URL = import.meta.env.PROD ? "" : "/api";
const AUTH_TOKEN = import.meta.env.VITE_DEV_TOKEN || "student-token";

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

/**
 * Sample resources, shaped like the real ones will be.
 * "All Students" in majors = shown for every major.
 * The pending one is there to prove the page hides anything not approved.
 */
const SAMPLE_RESOURCES = [
  { id: 1, title: "Tech resume checklist", description: "One-page format, project bullets that show impact, and the skills section recruiters scan first.", type: "Resume & Cover Letter", industry: "Technology", link: "#", majors: ["CS Majors", "IT Majors"], sharedBy: "Career Services", status: "approved" },
  { id: 2, title: "Behavioral question bank (STAR)", description: "Common \"tell me about a time\" questions with a STAR worksheet to plan your stories.", type: "Interview Questions", industry: "All industries", link: "#", majors: ["All Students"], sharedBy: "Career Services", status: "approved" },
  { id: 3, title: "Technical interview prep: data structures", description: "Arrays, hash maps, trees and graphs: what to review and how to talk through your approach.", type: "Interview Questions", industry: "Technology", link: "#", majors: ["CS Majors"], sharedBy: "Student submission", status: "approved" },
  { id: 4, title: "Negotiating your first offer", description: "How to research a range, what to say on the call, and what else you can ask for besides salary.", type: "Salary Negotiation", industry: "All industries", link: "#", majors: ["All Students"], sharedBy: "Career Services", status: "approved" },
  { id: 5, title: "30-second pitch templates", description: "Fill-in templates for career fairs and info sessions, with examples for different class years.", type: "Elevator Pitch", industry: "All industries", link: "#", majors: ["All Students"], sharedBy: "Career Services", status: "approved" },
  { id: 6, title: "Applying for government internships", description: "Federal resumes, job-board searches and security clearance basics, step by step.", type: "Application Tips", industry: "Government", link: "#", majors: ["CS Majors", "IT Majors"], sharedBy: "Career Services", status: "approved" },
  { id: 7, title: "Cover letter template for IT support roles", description: "A short template that connects help-desk and lab experience to the job posting.", type: "Resume & Cover Letter", industry: "Technology", link: "#", majors: ["IT Majors"], sharedBy: "Student submission", status: "approved" },
  { id: 8, title: "Practice interviews with a peer", description: "Pair up with another student for a mock interview and get private feedback afterwards.", type: "Practice Tools", industry: "All industries", link: "#", majors: ["All Students"], sharedBy: "Career Services", status: "approved" },
  { id: 9, title: "Landing your first internship", description: "Where freshmen and sophomores can find early programs, and what to put on a resume with little experience.", type: "Application Tips", industry: "All industries", link: "#", majors: ["Freshmen", "Sophomores"], sharedBy: "Career Services", status: "approved" },
  { id: 10, title: "Senior job search timeline", description: "When full-time recruiting opens, how to track offers, and how to handle deadlines that overlap.", type: "Application Tips", industry: "All industries", link: "#", majors: ["Juniors", "Seniors"], sharedBy: "Student submission", status: "approved" },
  { id: 11, title: "Explaining your research to employers", description: "How to describe a thesis or research project to industry interviewers in plain language.", type: "Interview Questions", industry: "Research", link: "#", majors: ["Graduate Students"], sharedBy: "Career Services", status: "approved" },
  { id: 12, title: "Pending sample (should NOT show)", description: "Waiting for admin approval.", type: "Practice Tools", industry: "All industries", link: "#", majors: ["All Students"], sharedBy: "Student submission", status: "pending" },
];

/**
 * Small fetch helper for this file (eventsApi.js keeps its own; we don't edit it).
 * Throws an Error with .status and .code from our {"error","code","detail"} format.
 */
async function request(path) {
  let response;
  try {
    response = await fetch(BASE_URL + path, { headers: { Authorization: `Bearer ${AUTH_TOKEN}` } });
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

/** Turns one resource from the server into the shape the cards use. Adjust field names here. */
function toCard(r) {
  return {
    id: r.id,
    title: r.title,
    description: r.description ?? "",
    type: r.type ?? "",
    industry: r.industry ?? "All industries",
    link: r.link ?? "#",
    majors: r.majors ?? r.audiences ?? ["All Students"],
    sharedBy: r.sharedBy ?? r.owner ?? "Career Services",
    status: r.status ?? "approved",
  };
}

export async function getResources(filters = {}) {
  if (USE_SAMPLE_RESOURCES) {
    return SAMPLE_RESOURCES.filter((r) => matchesFilters(r, filters));
  }
  const params = new URLSearchParams();
  if (filters.q) params.set("q", filters.q);
  if (filters.major && filters.major !== "All majors") params.set("major", filters.major);
  if (filters.type && filters.type !== "All types") params.set("type", filters.type);
  const page = await request(`/resources?${params}`);
  return page.items.map(toCard);
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
