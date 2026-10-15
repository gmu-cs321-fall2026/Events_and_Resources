import { useState } from "react";
import CreateEventPage from "./pages/CreateEventPage";
import EventsPage from "./pages/EventsPage";

function App() {
	//A counter that goes up every time an event is created.
	//EventsPage reloads its list whenever this number changes, so a new event shows up right away without refreshing the browser.
	const [refreshKey, setRefreshKey] = useState(0);
	
	return (
		<div>
			<h1>Career Events & Resources</h1>
			<p>Welcome to our Career Events page!</p>
			<CreateEventPage onCreated={() => setRefreshKey((n) => n + 1)} />
				<EventsPage refreshKey={refreshKey} />
		</div>
	)
}

export default App