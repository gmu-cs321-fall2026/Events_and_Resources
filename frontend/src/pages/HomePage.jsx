/**
 * HomePage (MASON-50 Website navigation + MASON-10 Search bar): the landing page.
 *
 * With the search box empty, three previews, each with a "See all" link:
 *   Coming up         the next 4 events from today        -> #events
 *   Apply soon        the 3 open jobs closing soonest     -> #jobs
 *   Prep resources    3 resources                         -> #resources
 * When a student types in the search box, the same three sections show what matches
 * instead: upcoming events (GET /events?q=), open jobs (GET /opportunities?q=) and
 * resources (getResources({ q })). One search box, everything on the site.
 *
 * Clicking an event or a job opens the same pop-up as on its own page, so a student
 * can register for an event or open a job's Apply link without leaving Home.
 *
 * State:
 *   query                   = what's typed in the search box right now
 *   keyword                 = query, trimmed, 300 ms after the last keystroke ("" = no search)
 *   events, jobs, resources = { key, items, error }: key is the keyword these items are for
 *   openEventId, openJobId  = the pop-up that's open, or null
 */
import { useEffect, useState } from "react";
import { getEvents } from "../api/eventsApi";
import { getOpportunities } from "../api/opportunitiesApi";
import { getResources } from "../api/resourcesApi";
import EventDetailModal from "../components/EventDetailModal";
import JobCard from "../components/JobCard";
import JobDetailModal from "../components/JobDetailModal";
import ResourceCard from "../components/ResourceCard";
import "./HomePage.css";
import "./JobsPage.css";

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const NOT_LOADED = { key: null, items: [], error: null };

/** How long to wait after the last keystroke before searching (milliseconds). */
const TYPING_DELAY = 300;

/** "15:00" -> "3:00 PM" */
function formatTime(hhmm) {
	const [h, m] = hhmm.split(":").map(Number);
	return `${((h + 11) % 12) + 1}:${String(m).padStart(2, "0")} ${h >= 12 ? "PM" : "AM"}`;
}

export default function HomePage() {
	const [query, setQuery] = useState("");
	const [keyword, setKeyword] = useState("");
	const [events, setEvents] = useState(NOT_LOADED);
	const [jobs, setJobs] = useState(NOT_LOADED);
	const [resources, setResources] = useState(NOT_LOADED);
	const [openEventId, setOpenEventId] = useState(null);
	const [openJobId, setOpenJobId] = useState(null);

	/**
	 * Waits until the student stops typing, then updates keyword. So "resume" sends one
	 * search instead of six. The cleanup cancels the wait when another key is pressed.
	 */
	useEffect(() => {
		const timer = setTimeout(() => setKeyword(query.trim()), TYPING_DELAY);
		return () => clearTimeout(timer);
	}, [query]);

	/**
	 * Loads the three sections whenever keyword changes ("" = the normal previews).
	 * Each one has its own .then/.catch, so if one fails the other two still show.
	 * The ignore flag drops answers for an old keyword that arrive after newer ones.
	 */
	useEffect(() => {
		let ignore = false;
		const searching = keyword !== "";
		const today = new Date().toLocaleDateString("en-CA");

		getEvents({ from: today, q: keyword, pageSize: searching ? 6 : 4 })
			.then((page) => {
				if (!ignore) setEvents({ key: keyword, items: page.items, error: null });
			})
			.catch((err) => {
				if (!ignore) setEvents({ key: keyword, items: [], error: err.message });
			});

		getOpportunities({ q: keyword, pageSize: searching ? 6 : 3 })
			.then((page) => {
				if (!ignore) setJobs({ key: keyword, items: page.items, error: null });
			})
			.catch((err) => {
				if (!ignore) setJobs({ key: keyword, items: [], error: err.message });
			});

		getResources({ q: keyword })
			.then((items) => {
				if (!ignore) setResources({ key: keyword, items: items.slice(0, searching ? 6 : 3), error: null });
			})
			.catch((err) => {
				if (!ignore) setResources({ key: keyword, items: [], error: err.message });
			});

		return () => {
			ignore = true;
		};
	}, [keyword]);

	const searching = keyword !== "";
	const allLoaded = [events, jobs, resources].every((s) => s.key === keyword);
	const nothingFound = searching && allLoaded && [events, jobs, resources].every((s) => s.items.length === 0);

	return (
		<div className="home">
			<section className="home-hero">
				<span className="home-hero__eyebrow">George Mason University · Career Services</span>
				<h1 className="home-hero__title">Your career search, all in one place.</h1>
				<p className="home-hero__lead">
					Find career fairs and workshops, prep with guides picked by Career Services,
					and apply to internships and jobs before the deadline.
				</p>

				<label className="home-search">
					<svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
					<span className="visually-hidden">Search events, jobs and resources</span>
					<input
						type="search"
						value={query}
						onChange={(e) => setQuery(e.target.value)}
						placeholder='Search events, jobs and resources (e.g. "resume", "Capital One")'
					/>
				</label>

				{!searching && (
					<div className="home-tiles">
						<a className="home-tile" href="#events">
							<span className="home-tile__title">Events</span>
							<span className="home-tile__text">Career fairs, workshops and info sessions. Register in one click.</span>
						</a>
						<a className="home-tile" href="#resources">
							<span className="home-tile__title">Resources</span>
							<span className="home-tile__text">Resume guides, interview questions and salary tips for your major.</span>
						</a>
						<a className="home-tile" href="#jobs">
							<span className="home-tile__title">Jobs &amp; Internships</span>
							<span className="home-tile__text">Openings posted by Career Services, with the link to apply.</span>
						</a>
					</div>
				)}
			</section>

			{nothingFound && (
				<p className="home-nothing" role="status">
					Nothing matches “{keyword}”. Try a shorter word, or browse <a href="#events">events</a>,{" "}
					<a href="#resources">resources</a> or <a href="#jobs">jobs</a>.
				</p>
			)}

			<HomeSection
				title={searching ? `Events matching “${keyword}”` : "Coming up"}
				seeAllHref="#events" seeAllText="See the full calendar" state={events} keyword={keyword}
				emptyText={searching ? "No upcoming events match." : "No upcoming events yet."}
			>
				<ul className="home-events">
					{events.items.map((e) => {
						const [, month, day] = e.date.split("-");
						return (
							<li key={e.id}>
								<button type="button" className="home-event" onClick={() => setOpenEventId(e.id)}>
									<span className="home-event__date">
										<span className="home-event__month">{MONTHS[Number(month) - 1]}</span>
										<span className="home-event__day">{Number(day)}</span>
									</span>
									<span className="home-event__text">
										<span className="home-event__category">{e.category}</span>
										<span className="home-event__title">{e.eventTitle}</span>
										<span className="home-event__when">{formatTime(e.time)} · {e.location}</span>
									</span>
								</button>
							</li>
						);
					})}
				</ul>
			</HomeSection>

			<HomeSection
				title={searching ? `Jobs matching “${keyword}”` : "Apply soon"}
				seeAllHref="#jobs" seeAllText="See all jobs & internships" state={jobs} keyword={keyword}
				emptyText={searching ? "No open jobs match." : "No open postings right now."}
			>
				<ul className="job-grid">
					{jobs.items.map((job) => (
						<JobCard key={job.id} job={job} onSelect={() => setOpenJobId(job.id)} />
					))}
				</ul>
			</HomeSection>

			<HomeSection
				title={searching ? `Resources matching “${keyword}”` : "Prep resources"}
				seeAllHref="#resources" seeAllText="Browse all resources" state={resources} keyword={keyword}
				emptyText={searching ? "No resources match." : "No resources yet."}
			>
				<div className="home-resources">
					{resources.items.map((r) => (
						<ResourceCard key={r.id} resource={r} />
					))}
				</div>
			</HomeSection>

			{openEventId !== null && (
				<EventDetailModal key={openEventId} eventId={openEventId} onClose={() => setOpenEventId(null)} />
			)}
			{openJobId !== null && (
				<JobDetailModal key={openJobId} jobId={openJobId} onClose={() => setOpenJobId(null)} />
			)}
		</div>
	);
}

/**
 * One section: a heading with a "See all" link, then "Loading…", an error, the empty
 * text, or the content passed inside it (children). It counts as loading while its
 * items are for a different keyword than the one being searched.
 */
function HomeSection({ title, seeAllHref, seeAllText, state, keyword, emptyText, children }) {
	let body;
	if (state.key !== keyword) body = <p className="home-section__note">Loading…</p>;
	else if (state.error) body = <p className="home-section__note home-section__note--error">Couldn't load this: {state.error}</p>;
	else if (state.items.length === 0) body = <p className="home-section__note">{emptyText}</p>;
	else body = children;

	return (
		<section className="home-section" aria-label={title}>
			<div className="home-section__head">
				<h2>{title}</h2>
				<a className="home-section__all" href={seeAllHref}>{seeAllText} →</a>
			</div>
			{body}
		</section>
	);
}