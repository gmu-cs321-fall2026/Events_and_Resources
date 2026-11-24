/**
 * App: the top of the React app.
 *
 * MASON-50 (Website navigation): the navigation bar and the login bar, then ONE page
 * at a time, picked from the "#" part of the URL by useHashRoute():
 *   #home (default)  HomePage        a quick look at events, jobs and resources, with search
 *   #events          EventsHubPage   calendar/list (+ Create Event for staff and admin)
 *   #resources       ResourcesPage   resource directory (+ share, My submissions, review queue)
 *   #jobs            JobsPage        jobs & internships
 * Links anywhere on the site just point at those hashes, e.g. "See all jobs" -> #jobs.
 * "#calendar" (the Resources page's "Open in calendar") also opens Events.
 *
 * LoginBar (Trang) stays on every page, so you can log in or out from anywhere.
 */
import { useState } from "react";
import LoginBar from "./components/LoginBar";
import NavBar from "./components/NavBar";
import ShareResourceModal from "./components/ShareResourceModal";
import MyResourcesPanel from "./components/MyResourcesPanel";
import ResourceReviewPanel from "./components/ResourceReviewPanel";
import EventsHubPage from "./pages/EventsHubPage";
import HomePage from "./pages/HomePage";
import JobsPage from "./pages/JobsPage";
import ResourcesPage from "./pages/ResourcesPage";
import { useAuth } from "./auth/useAuth";
import { useHashRoute } from "./utils/hashRoute";

/**
 * State (only the Resources page needs any, for MASON-7 / MASON-11):
 *   sharing      = true while the "Share a resource" pop-up is open
 *   resourcesKey = goes up after a resource is shared, edited, deleted, approved or denied,
 *                  so the directory, "My submissions" and the review queue all reload
 *
 * useAuth(): "Share a resource" and "My submissions" are for anyone logged in;
 * the review queue is only for staff and admin. The backend checks roles again.
 */
function App() {
	const page = useHashRoute();
	const { user, isStaff } = useAuth();
	const [sharing, setSharing] = useState(false);
	const [resourcesKey, setResourcesKey] = useState(0);
	const resourcesChanged = () => setResourcesKey((n) => n + 1);

	return (
		<>
			<NavBar current={page} />
			<LoginBar />
			<main>
				{page === "home" && <HomePage />}
				{page === "events" && <EventsHubPage />}
				{page === "resources" && (
					<>
						<ResourcesPage
							calendarHref="#calendar"
							jobsHref="#jobs"
							reloadKey={resourcesKey}
							onShare={user ? () => setSharing(true) : undefined}
						/>
						{user && <MyResourcesPanel reloadKey={resourcesKey} onChanged={resourcesChanged} />}
						{isStaff && <ResourceReviewPanel onChanged={resourcesChanged} />}
					</>
				)}
				{page === "jobs" && <JobsPage />}
			</main>

			{sharing && (
				<ShareResourceModal
					onClose={() => setSharing(false)}
					onSaved={() => {
						setSharing(false);
						resourcesChanged();
					}}
				/>
			)}
		</>
	);
}

export default App;