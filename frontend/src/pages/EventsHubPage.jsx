/**
 * EventsHubPage (MASON-50 Website navigation): everything about events on one page.
 *
 * Before navigation, these pieces sat one after another in App.jsx. Now they live
 * here, under the "Events" link:
 *   - CreateEventPage (Create Event form), shown ONLY when staff or admin is logged in
 *   - a Calendar / List switch
 *   - CalendarPage (Trang, MASON-3) or EventsPage (the list)
 * None of those files changed: this page only arranges them.
 *
 * isStaff comes from useAuth() (Trang's login): true when staff or admin is logged in.
 *
 * State:
 *   refreshKey = goes up after an event is created, so the calendar/list reloads
 *   eventsView = "calendar" (default) or "list"
 */
import { useState } from "react";
import { useAuth } from "../auth/useAuth";
import CalendarPage from "./CalendarPage";
import CreateEventPage from "./CreateEventPage";
import EventsPage from "./EventsPage";

export default function EventsHubPage() {
	const { isStaff } = useAuth();
	const [refreshKey, setRefreshKey] = useState(0);
	const [eventsView, setEventsView] = useState("calendar");

	return (
		<div>
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

			{isStaff && <CreateEventPage onCreated={() => setRefreshKey((n) => n + 1)} />}
		</div>
	);
}