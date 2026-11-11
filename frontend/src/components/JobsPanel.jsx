/**
 * JobsPanel: "Jobs & internships", the three open postings with the soonest
 * deadlines, next to the resources.
 *
 * Uses REAL data since MASON-13: GET /opportunities?pageSize=3 (the backend sorts by
 * deadline and hides closed postings).
 *
 * Props:
 *   jobsHref  = where the rows and "See all" go (the Jobs page, "#jobs")
 *   reloadKey = optional; when it changes the panel loads again (e.g. after staff post a job)
 */
import { useEffect, useState } from "react";
import { formatDeadline, getOpportunities } from "../api/opportunitiesApi";
import "./SidePanels.css";

export default function JobsPanel({ jobsHref = "#jobs", reloadKey = 0 }) {
  const [result, setResult] = useState({ key: null, items: [], error: null });

  /** The ignore flag drops an old answer that arrives after a newer one. */
  useEffect(() => {
    let ignore = false;
    getOpportunities({ pageSize: 3 })
      .then((page) => {
        if (!ignore) setResult({ key: reloadKey, items: page.items, error: null });
      })
      .catch((err) => {
        if (!ignore) setResult({ key: reloadKey, items: [], error: err.message });
      });
    return () => {
      ignore = true;
    };
  }, [reloadKey]);

  const loading = result.key !== reloadKey;
  const jobs = result.items;

  return (
    <section className="side-panel" aria-labelledby="jobs-heading">
      <span className="side-panel__eyebrow">Jobs &amp; internships</span>
      <h2 id="jobs-heading" className="side-panel__title">Apply soon</h2>

      {loading && <p className="side-panel__note">Loading postings…</p>}
      {!loading && result.error && (
        <p className="side-panel__note side-panel__note--error">Couldn't load postings: {result.error}</p>
      )}
      {!loading && !result.error && jobs.length === 0 && (
        <p className="side-panel__note">No open postings right now.</p>
      )}

      {!loading && !result.error && jobs.length > 0 && (
        <ul className="side-panel__list">
          {jobs.map((job) => (
            <li key={job.id}>
              <a className="job-row" href={jobsHref}>
                <span className="job-row__title">{job.title}</span>
                <span className="job-row__employer">{job.employer} · {job.type}</span>
                <span className="job-row__deadline">Apply by {formatDeadline(job.deadline)}</span>
              </a>
            </li>
          ))}
        </ul>
      )}

      <a className="side-panel__button" href={jobsHref}>See all jobs &amp; internships</a>
    </section>
  );
}