import { useState } from "react";
import CreateEventPage from "./pages/CreateEventPage";
import EventsPage from "./pages/EventsPage";
import CalendarPage from "./pages/CalendarPage";

function App() {
	// A counter that goes up every time an event is created.
	// EventsPage reloads its list whenever this number changes, so a new event shows up right away without refreshing the browser.
	//
	// Ways to show events: "calendar" (default) or "list"
	// Both read the same GET/events data
	const [refreshKey, setRefreshKey] = useState(0);
	const [eventsView, setEventsView] = useState("calendar");
	
	return (
		<div>
			<h1>Career Events & Resources</h1>
			<p>Welcome to our Career Events page!</p>
			<CreateEventPage onCreated={() => setRefreshKey((n) => n + 1)} />
			
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
		</div>
	)
}

export default App