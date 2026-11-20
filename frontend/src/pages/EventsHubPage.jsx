/**
 * EventsHubPage (MASON-50 Website navigation + MASON-10 Search bar): the Events tab.
 *
 * Laid out like the Resources page so the two tabs feel like one site:
 *   1. Green hero: title, search box and "Browse by type" category chips.
 *      Staff and admin also get a "Create an event" button there.
 *   2. Body, in two columns:
 *      left  = heading, Calendar / List switch, format pills (All / In-person / Virtual),
 *              then CalendarPage (Trang, MASON-3) or EventsPage (the list)
 *      right = two cards (EventSidePanels.jsx): "Coming up" (next matching events)
 *              and "Your events" (what you registered for). Clicking a row opens the
 *              same event pop-up as the calendar.
 *   3. CreateEventPage (Create Event form), shown ONLY when staff or admin is logged in.
 *
 * It reuses the Resources page's styles (hero, search box, chips, pills) from
 * ResourcesPage.css, so a color change there changes both pages. EventsHubPage.css
 * only adds what's new: the view switch and green/gold calendar colors.
 *
 * isStaff comes from useAuth() (Trang's login): true when staff or admin is logged in.
 *
 * State:
 *   refreshKey = goes up after an event is created, so the calendar/list reloads
 *   eventsView = "calendar" (default) or "list"
 *   query      = what's typed in the search box right now
 *   keyword    = query, trimmed, 300 ms after the last keystroke ("" = no search)
 *   category   = the category chip that's on ("All events" = every category)
 *   format     = the format pill that's on ("All formats" = both)
 *   openEventId = the event whose pop-up was opened from a side card (null = none)
 *   sideKey    = goes up when ANY event pop-up closes (side card, calendar or list),
 *                so seats and "Your events" reload after registering or cancelling
 *
 * The filter state lives HERE (the parent), so it stays the same when the student
 * switches between Calendar and List.
 */
import { useEffect, useRef, useState } from "react";
import { useAuth } from "../auth/useAuth";
import EventDetailModal from "../components/EventDetailModal";
import { MyEventsPanel, NextEventsPanel } from "../components/EventSidePanels";
import gmuLogo from "../assets/gmu-logo.png";
import {
  ALL_CATEGORIES,
  ALL_FORMATS,
  CATEGORY_CHIPS,
  FORMAT_PILLS,
  toApiFilters,
} from "../utils/eventSearch";
import CalendarPage from "./CalendarPage";
import CreateEventPage from "./CreateEventPage";
import EventsPage from "./EventsPage";
import "./ResourcesPage.css";
import "./EventsHubPage.css";

/** How long to wait after the last keystroke before searching (same as HomePage). */
const TYPING_DELAY = 300;

/** Heading above the calendar for each category chip. */
const HEADINGS = {
	[ALL_CATEGORIES]: "All events",
	"Career Fair": "Career fairs",
	Workshop: "Workshops",
	"Employer Info Session": "Employer info sessions",
	Networking: "Networking events",
	Panel: "Panels",
	Internal: "Internal events",
};

export default function EventsHubPage() {
	const { isStaff } = useAuth();
	const [refreshKey, setRefreshKey] = useState(0);
	const [eventsView, setEventsView] = useState("calendar");
	const [query, setQuery] = useState("");
	const [keyword, setKeyword] = useState("");
	const [category, setCategory] = useState(ALL_CATEGORIES);
	const [format, setFormat] = useState(ALL_FORMATS);
	const [openEventId, setOpenEventId] = useState(null);
	const [sideKey, setSideKey] = useState(0);
	const createFormRef = useRef(null);

	/**
	 * Waits until the student stops typing, then updates keyword, so "resume" sends one
	 * request instead of six. The cleanup cancels the wait when another key is pressed.
	 */
	useEffect(() => {
		const timer = setTimeout(() => setKeyword(query.trim()), TYPING_DELAY);
		return () => clearTimeout(timer);
	}, [query]);

	const filters = toApiFilters({ keyword, category, format });

	/** Puts every filter back to "show everything". */
	function clearFilters() {
		setQuery("");
		setKeyword("");
		setCategory(ALL_CATEGORIES);
		setFormat(ALL_FORMATS);
	}

	/** Reloads both side cards (after a pop-up closes: seats or registrations may have changed). */
	function reloadSideCards() {
		setSideKey((n) => n + 1);
	}

	/** Scrolls down to the Create Event form (staff/admin only). */
	function goToCreateForm() {
		createFormRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
	}

	return (
		<div className="resources-page events-page">
			{/* ---------- 1. hero ---------- */}
			<section className="resources-hero">
				<div className="hero-logo">
					<img src={gmuLogo} alt="George Mason University" />
				</div>
				<div className="resources-hero__top">
					<span className="resources-hero__eyebrow">Career Events</span>
					{isStaff && (
						<button type="button" className="share-button" onClick={goToCreateForm}>
							<svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M12 5v14M5 12h14" /></svg>
							Create an event
						</button>
					)}
				</div>

				<h1 className="resources-hero__title">Show up where it counts.</h1>
				<p className="resources-hero__lead">
					Career fairs, employer info sessions, workshops and networking nights from Career
					Services. Search or pick a type, then register in one click.
				</p>

				<label className="search-box">
					<svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
					<span className="visually-hidden">Search events</span>
					<input
						type="search"
						value={query}
						onChange={(e) => setQuery(e.target.value)}
						maxLength={100}
						placeholder='Search by title, host, location or audience (e.g. "resume")'
					/>
				</label>

				<div className="chip-group">
					<span id="category-label" className="chip-group__label">Browse by type</span>
					<div role="group" aria-labelledby="category-label" className="chip-row">
						{CATEGORY_CHIPS.map((c) => (
							<button
								key={c}
								type="button"
								className={c === category ? "chip chip--on" : "chip"}
								aria-pressed={c === category}
								onClick={() => setCategory(c)}
							>
								{c}
							</button>
						))}
					</div>
				</div>
			</section>

			{/* ---------- 2. body: main column + side cards ---------- */}
			<div className="events-body">
				<div className="events-columns">
					<div className="events-main">
						<div className="results-head">
							<h2>{HEADINGS[category] ?? category}</h2>
							<div className="events-view-switch" role="group" aria-label="Show events as">
								<button type="button" aria-pressed={eventsView === "calendar"} onClick={() => setEventsView("calendar")}>
									Calendar
								</button>
								<button type="button" aria-pressed={eventsView === "list"} onClick={() => setEventsView("list")}>
									List
								</button>
							</div>
						</div>

						<div role="group" aria-label="Event format" className="pill-row">
							{FORMAT_PILLS.map((f) => (
								<button
									key={f}
									type="button"
									className={f === format ? "pill pill--on" : "pill"}
									aria-pressed={f === format}
									onClick={() => setFormat(f)}
								>
									{f}
								</button>
							))}
						</div>

						<div className="events-panel">
							{eventsView === "calendar" ? (
								<CalendarPage
									refreshKey={refreshKey}
									filters={filters}
									onShowList={() => setEventsView("list")}
									onModalClose={reloadSideCards}
								/>
							) : (
								<EventsPage
									refreshKey={refreshKey}
									filters={filters}
									onClear={clearFilters}
									onModalClose={reloadSideCards}
								/>
							)}
						</div>
					</div>

					<aside className="events-side" aria-label="Event highlights">
						<NextEventsPanel
							filters={filters}
							reloadKey={refreshKey + sideKey}
							onOpen={setOpenEventId}
							onSeeAll={() => setEventsView("list")}
						/>
						<MyEventsPanel reloadKey={sideKey} onOpen={setOpenEventId} />
					</aside>
				</div>

				{/* ---------- 3. Create Event (staff/admin) ---------- */}
				{isStaff && (
					<div ref={createFormRef} className="events-create">
						<CreateEventPage onCreated={() => setRefreshKey((n) => n + 1)} />
					</div>
				)}
			</div>

			{/* Pop-up for an event clicked in a side card. key = a fresh pop-up per event. */}
			{openEventId !== null && (
				<EventDetailModal
					key={openEventId}
					eventId={openEventId}
					onClose={() => {
						setOpenEventId(null);
						reloadSideCards();
					}}
				/>
			)}
		</div>
	);
}
