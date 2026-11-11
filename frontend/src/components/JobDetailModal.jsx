/**
 * JobDetailModal (MASON-13): the pop-up that shows one posting in full.
 *
 * Students see the details and an "Apply" link to the employer's own site.
 * Staff also see Edit (swaps the details for JobForm) and Delete (asks
 * "are you sure?" first, because a delete can't be undone).
 *
 * Props:
 *   jobId     = which posting to show
 *   onClose   = closes the pop-up
 *   onChanged = called after an edit or delete, so the list can reload
 *
 * State:
 *   job        = the posting once it arrives (null = still loading)
 *   mode       = "view", "edit" or "confirmDelete"
 *   error      = a message to show, e.g. "This posting no longer exists."
 */
import { useEffect, useState } from "react";
import { IS_STAFF, deleteOpportunity, formatDeadline, getOpportunity } from "../api/opportunitiesApi";
import JobForm from "./JobForm";
import "./EventDetailModal.css";
import "../pages/JobsPage.css";

export default function JobDetailModal({ jobId, onClose, onChanged }) {
	const [job, setJob] = useState(null);
	const [mode, setMode] = useState("view");
	const [error, setError] = useState(null);

	/** Loads the posting when the pop-up opens (the ignore flag drops a stale answer). */
	useEffect(() => {
		let ignore = false;
		getOpportunity(jobId)
			.then((data) => {
				if (!ignore) setJob(data);
			})
			.catch((err) => {
				if (!ignore) setError(err.status === 404 ? "This posting no longer exists." : err.message);
			});
		return () => {
			ignore = true;
		};
	}, [jobId]);

	/** Escape closes the pop-up. The cleanup removes the listener when it closes. */
	useEffect(() => {
		function handleKeyDown(e) {
			if (e.key === "Escape") onClose();
		}
		window.addEventListener("keydown", handleKeyDown);
		return () => window.removeEventListener("keydown", handleKeyDown);
	}, [onClose]);

	async function handleDelete() {
		try {
			await deleteOpportunity(jobId);
			onChanged?.();
			onClose();
		} catch (err) {
			setError(err.message);
			setMode("view");
		}
	}

	let content;
	if (!job) {
		content = error ? <p className="error">{error}</p> : <p>Loading posting…</p>;
	} else if (mode === "edit") {
		content = (
			<JobForm
				job={job}
				onSaved={(saved) => {
					setJob(saved);
					setMode("view");
					onChanged?.();
				}}
				onCancel={() => setMode("view")}
			/>
		);
	} else {
		content = (
			<>
				<span className="event-detail-tag">{job.type}</span>
				<h2 id="job-detail-title">{job.title}</h2>

				<dl className="event-detail-fields">
					<dt>Employer</dt>
					<dd>{job.employer}</dd>
					<dt>Apply by</dt>
					<dd>{formatDeadline(job.deadline)}</dd>
					<dt>Posted</dt>
					<dd>{formatDeadline(job.datePosted)}</dd>
				</dl>

				<p className="job-detail__description">{job.description}</p>

				{error && <p className="error" role="alert">{error}</p>}

				{/* rel="noopener noreferrer": the employer's page can't reach back into ours. */}
				<a className="job-button job-apply" href={job.link} target="_blank" rel="noopener noreferrer">
					Apply on the employer's site
				</a>

				{IS_STAFF && mode === "view" && (
					<div className="job-staff-actions">
						<button type="button" className="job-button job-button--plain" onClick={() => setMode("edit")}>Edit</button>
						<button type="button" className="job-button job-button--danger" onClick={() => setMode("confirmDelete")}>Delete</button>
					</div>
				)}

				{IS_STAFF && mode === "confirmDelete" && (
					<div className="job-staff-actions" role="alert">
						<span>Delete this posting? This can't be undone.</span>
						<button type="button" className="job-button job-button--plain" onClick={() => setMode("view")}>Keep it</button>
						<button type="button" className="job-button job-button--danger" onClick={handleDelete}>Yes, delete</button>
					</div>
				)}
			</>
		);
	}

	return (
		<div className="modal-backdrop" onClick={onClose}>
			<div
				className="modal job-modal"
				role="dialog"
				aria-modal="true"
				aria-labelledby={mode === "edit" ? "job-form-title" : "job-detail-title"}
				onClick={(e) => e.stopPropagation()}
			>
				<button type="button" className="modal-close" onClick={onClose} aria-label="Close" autoFocus>
					×
				</button>
				{content}
			</div>
		</div>
	);
}