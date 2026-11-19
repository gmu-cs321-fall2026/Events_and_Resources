/**
 * App: the top of the React app.
 *
 * MASON-50 (Website navigation): the navigation bar and the login bar, then ONE page
 * at a time, picked from the "#" part of the URL by useHashRoute():
 *   #home (default)  HomePage        a quick look at events, jobs and resources, with search
 *   #events          EventsHubPage   calendar/list (+ Create Event for staff and admin)
 *   #resources       ResourcesPage   resource directory
 *   #jobs            JobsPage        jobs & internships
 * Links anywhere on the site just point at those hashes, e.g. "See all jobs" -> #jobs.
 * "#calendar" (the Resources page's "Open in calendar") also opens Events.
 *
 * LoginBar (Trang) stays on every page, so you can log in or out from anywhere.
 */
import LoginBar from "./components/LoginBar";
import NavBar from "./components/NavBar";
import EventsHubPage from "./pages/EventsHubPage";
import HomePage from "./pages/HomePage";
import JobsPage from "./pages/JobsPage";
import ResourcesPage from "./pages/ResourcesPage";
import { useHashRoute } from "./utils/hashRoute";

function App() {
	const page = useHashRoute();

	return (
		<>
			<NavBar current={page} />
			<LoginBar />
			<main>
				{page === "home" && <HomePage />}
				{page === "events" && <EventsHubPage />}
				{page === "resources" && <ResourcesPage calendarHref="#calendar" jobsHref="#jobs" />}
				{page === "jobs" && <JobsPage />}
			</main>
		</>
	);
}

export default App;