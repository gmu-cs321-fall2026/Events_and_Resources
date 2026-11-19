/**
 * ReminderToggle: the "Remind me the day before" checkbox in the event pop-up.
 * RegisterButton shows it only once you're registered.
 *
 * Props:
 *   eventId  = which event
 *   remind   = is the reminder on right now? (RegisterButton gets it from GET /events/{id}/register)
 *   onChange = called with true/false after the server saved the new choice
 *
 * The checkbox is "controlled": it always shows the remind prop, and only changes after
 * the server says the choice was saved. While the request is on its way it's disabled,
 * so a quick double click can't send two opposite requests.
 */
import { useState } from "react";
import { setReminder } from "../api/eventsApi";

export default function ReminderToggle({ eventId, remind, onChange }) {
	const [busy, setBusy] = useState(false);
	const [error, setError] = useState(null);

	async function handleChange(e) {
		const wanted = e.target.checked;
		setBusy(true);
		setError(null);
		try {
			const status = await setReminder(eventId, wanted);
			onChange(status.remind);
		} catch (err) {
			if (err.status === 401) {
				setError("Please log in to set a reminder.");
			} else {
				setError(err.message);
			}
		} finally {
			setBusy(false);
		}
	}

	return (
		<div className="reminder-toggle">
			<label>
				<input type="checkbox" checked={remind} onChange={handleChange} disabled={busy} />
				Remind me the day before
			</label>
			{error && (
				<p className="register-message" role="alert">
					{error}
				</p>
			)}
		</div>
	);
}
