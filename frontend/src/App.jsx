import ResourcesPage from "./pages/ResourcesPage";
import { useState } from "react";
import ShareResourceModal from "./components/ShareResourceModal";
import MyResourcesPanel from "./components/MyResourcesPanel";
import ResourceReviewPanel from "./components/ResourceReviewPanel";
import CreateEventPage from "./pages/CreateEventPage";
import EventsPage from "./pages/EventsPage";
import CalendarPage from "./pages/CalendarPage";
import JobsPage from "./pages/JobsPage";
import LoginBar from "./components/LoginBar";
import { useAuth } from "./auth/useAuth";

function App() {
	// A counter that goes up every time an event is created.
	// EventsPage reloads its list whenever this number changes, so a new event shows up right away without refreshing the browser.
	//
	// Ways to show events: "calendar" (default) or "list"
	// Both read the same GET/events data
	const [refreshKey, setRefreshKey] = useState(0);
	const [eventsView, setEventsView] = useState("calendar");
	/** Goes up after staff post, edit or delete a job, so the Resources page's jobs panel reloads. */
	const [jobsKey, setJobsKey] = useState(0);
	/**
	 * Resources (MASON-7 / MASON-11):
	 *   sharing       = true while the "Share a resource" pop-up is open
	 *   resourcesKey  = goes up after a resource is shared, edited, deleted, approved or denied,
	 *                   so the directory and "My submissions" both reload
	 */
	const [sharing, setSharing] = useState(false);
	const [resourcesKey, setResourcesKey] = useState(0);
	const resourcesChanged = () => setResourcesKey((n) => n + 1);
	const { user, isStaff } = useAuth();

	return (
		<div>
			<LoginBar />
			<ResourcesPage
				calendarHref="#calendar"
				jobsReloadKey={jobsKey}
				reloadKey={resourcesKey}
				onShare={user ? () => setSharing(true) : undefined}
			/>
			{sharing && (
				<ShareResourceModal
					onClose={() => setSharing(false)}
					onSaved={() => {
						setSharing(false);
						resourcesChanged();
					}}
				/>
			)}
			{user && <MyResourcesPanel reloadKey={resourcesKey} onChanged={resourcesChanged} />}
			{isStaff && <ResourceReviewPanel onChanged={resourcesChanged} />}
			<h1>Career Events & Resources</h1>
			<p>Welcome to our Career Events page!</p>
			{isStaff && <CreateEventPage onCreated={() => setRefreshKey((n) => n + 1)} />}

			<section id="calendar">
				<div className="events-view-switch" role="group" aria-label="Show events as">
					<button type="button" aria-pressed={eventsView === "calendar"} onClick={() => setEventsView("calendar")}>
						Calendar
					</button>
					<button type="button" aria-pressed={eventsView === "list"} onClick={() => setEventsView("list")}>
						List
					</button>
				</div>

				{eventsView === "calendar" ? (
					<CalendarPage refreshKey={refreshKey} />
				) : (
					<EventsPage refreshKey={refreshKey} />
				)}
			</section>
			<JobsPage onChanged={() => setJobsKey((n) => n + 1)} />
		</div>
	)
}

export default App
