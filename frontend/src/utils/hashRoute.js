/**
 * hashRoute.js (MASON-50 Website navigation): which page to show, read from the URL.
 *
 * The page lives in the part of the URL after "#":
 *   http://localhost:5173/#home        Home (a quick look at everything)
 *   http://localhost:5173/#events      Events calendar and list
 *   http://localhost:5173/#resources   Resources directory
 *   http://localhost:5173/#jobs        Jobs & internships
 *
 * Why the "#" part: the browser never sends it to the server, so Spring Boot doesn't
 * need any setup, a refresh stays on the same page, and Back/Forward work. Any link
 * like <a href="#jobs"> becomes navigation, with no extra code.
 */
import { useEffect, useState } from "react";

/** Every page, in the order the navigation bar shows them. The first one is the home page. */
export const PAGES = [
	{ id: "home", label: "Home", title: "Home" },
	{ id: "events", label: "Events", title: "Career Events" },
	{ id: "resources", label: "Resources", title: "Career Resources" },
	{ id: "jobs", label: "Jobs & Internships", title: "Jobs & Internships" },
];

/**
 * Other names that lead to a page. "#calendar" is the link Yasmin added for the
 * Resources page's "Open in calendar" button, and it means the Events page.
 */
const ALIASES = { calendar: "events" };

/**
 * "#jobs" -> "jobs". An empty or unknown hash (e.g. "", "#", "#oops") goes to the home page,
 * so a typo never shows a blank screen. Upper/lower case and a leading "/" are ignored ("#/Jobs").
 */
export function pageFromHash(hash) {
	const raw = (hash || "").replace(/^#\/?/, "").toLowerCase();
	const id = ALIASES[raw] ?? raw;
	return PAGES.some((p) => p.id === id) ? id : PAGES[0].id;
}

/**
 * React hook: the id of the page to show right now. It changes whenever the "#" part of
 * the URL changes (a link is clicked, or Back/Forward is pressed), and the page re-draws.
 * Each change also scrolls to the top and updates the browser tab's title.
 */
export function useHashRoute() {
	const [page, setPage] = useState(() => pageFromHash(window.location.hash));

	useEffect(() => {
		function handleHashChange() {
			setPage(pageFromHash(window.location.hash));
			window.scrollTo(0, 0);
		}
		window.addEventListener("hashchange", handleHashChange);
		return () => window.removeEventListener("hashchange", handleHashChange);
	}, []);

	useEffect(() => {
		const current = PAGES.find((p) => p.id === page);
		document.title = `${current.title} · Mason CareerLaunch`;
	}, [page]);

	return page;
}