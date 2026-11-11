/**
 * JobsPage (MASON-13 Post jobs): the jobs & internships bulletin board.
 *
 * Students: search and filter open postings, open one as a pop-up, and apply on the
 * employer's site. Staff (VITE_DEV_TOKEN=staff-token): also post, edit and delete.
 * Only postings whose deadline hasn't passed are shown (GET /opportunities default).
 *
 * Props:
 *   onChanged = optional, called after a post, edit or delete (e.g. so a side panel reloads)
 *
 * State:
 *   query, type      = the filters. Changing either re-runs the useEffect.
 *   reloadKey        = goes up after a post/edit/delete, so the list loads again
 *   result           = the last answer from the server, and which filters it was for
 *   showForm         = true while the staff "Post a job" form is open
 *   selectedJobId    = the posting whose pop-up is open, or null
 */
import { useEffect, useState } from "react";
import { IS_STAFF, JOB_TYPES, getOpportunities } from "../api/opportunitiesApi";
import JobCard from "../components/JobCard";
import JobDetailModal from "../components/JobDetailModal";
import JobForm from "../components/JobForm";
import "./JobsPage.css";

export default function JobsPage({ onChanged }) {
	const [query, setQuery] = useState("");
	const [type, setType] = useState("");
	const [reloadKey, setReloadKey] = useState(0);
	const [showForm, setShowForm] = useState(false);
	const [selectedJobId, setSelectedJobId] = useState(null);

	const filterKey = `${query.trim()}|${type}|${reloadKey}`;
	const [result, setResult] = useState({ key: null, items: [], totalCount: 0, error: null });

	/** Loads the list whenever a filter changes or a posting was changed. */
	useEffect(() => {
		let ignore = false;
		getOpportunities({ q: query, type, pageSize: 50 })
			.then((page) => {
				if (!ignore) setResult({ key: filterKey, items: page.items, totalCount: page.totalCount, error: null });
			})
			.catch((err) => {
				if (!ignore) setResult({ key: filterKey, items: [], totalCount: 0, error: err.message });
			});
		return () => {
			ignore = true;
		};
	}, [filterKey, query, type]);

	/** After any post, edit or delete: reload this list and tell the parent. */
	function handleChanged() {
		setReloadKey((n) => n + 1);
		onChanged?.();
	}

	const loading = result.key !== filterKey;
	const jobs = result.items;
	const count = `${result.totalCount} ${result.totalCount === 1 ? "posting" : "postings"}`;

	return (
		<section id="jobs" className="jobs-page" aria-labelledby="jobs-page-title">
			<div className="jobs-page__head">
				<div>
					<span className="jobs-page__eyebrow">Jobs &amp; internships</span>
					<h1 id="jobs-page-title">Opportunities posted by Career Services</h1>
				</div>
				{IS_STAFF && !showForm && (
					<button type="button" className="job-button" onClick={() => setShowForm(true)}>+ Post a job</button>
				)}
			</div>

			{IS_STAFF && showForm && (
				<div className="jobs-page__form">
					<JobForm
						onSaved={() => {
							setShowForm(false);
							handleChanged();
						}}
						onCancel={() => setShowForm(false)}
					/>
				</div>
			)}

			<div className="jobs-page__filters">
				<label className="jobs-search">
					<span className="visually-hidden">Search jobs and internships</span>
					<input
						type="search"
						value={query}
						onChange={(e) => setQuery(e.target.value)}
						placeholder='Search by title, employer or keyword (e.g. "intern")'
					/>
				</label>

				<div role="group" aria-label="Job type" className="job-pill-row">
					{["", ...JOB_TYPES].map((t) => (
						<button
							key={t || "all"}
							type="button"
							className={t === type ? "job-pill job-pill--on" : "job-pill"}
							aria-pressed={t === type}
							onClick={() => setType(t)}
						>
							{t || "All types"}
						</button>
					))}
				</div>
				<span className="jobs-page__count" aria-live="polite">{loading ? "Loading…" : count}</span>
			</div>

			{!loading && result.error && <p className="job-error" role="alert">Couldn't load postings: {result.error}</p>}

			{!loading && !result.error && jobs.length === 0 && (
				<p className="jobs-page__empty">No open postings match. Try another type or clear your search.</p>
			)}

			{!result.error && jobs.length > 0 && (
				<ul className="job-grid">
					{jobs.map((job) => (
						<JobCard key={job.id} job={job} onSelect={() => setSelectedJobId(job.id)} />
					))}
				</ul>
			)}

			{selectedJobId !== null && (
				<JobDetailModal
					key={selectedJobId}
					jobId={selectedJobId}
					onClose={() => setSelectedJobId(null)}
					onChanged={handleChanged}
				/>
			)}
		</section>
	);
}