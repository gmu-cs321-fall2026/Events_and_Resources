/**
 * JobCard (MASON-13): one posting in the jobs list. Clicking it opens the pop-up.
 *
 * Props:
 *   job      = the posting { title, employer, type, deadline, ... }
 *   onSelect = function JobsPage gives me; I call it when the card is clicked
 */
import { formatDeadline } from "../api/opportunitiesApi";

export default function JobCard({ job, onSelect }) {
	return (
		<li>
			<button type="button" className="job-card" onClick={onSelect}>
				<span className="job-type">{job.type}</span>
				<span className="job-card__title">{job.title}</span>
				<span className="job-card__employer">{job.employer}</span>
				<span className="job-card__deadline">Apply by {formatDeadline(job.deadline)}</span>
			</button>
		</li>
	);
}