/**
 * JobsPanel: "Jobs & internships", a short list next to the resources.
 *
 * PLACEHOLDER until Cindy's MASON-13 (GET /opportunities) is on main.
 * Then: load her list here (like UpcomingEventsPanel does with events) and
 * replace PLACEHOLDER_JOBS. The rest of the page doesn't change.
 *
 * Props:
 *   jobsHref = where "See all" goes ("#" until her page/tab exists)
 */
import "./SidePanels.css";

const PLACEHOLDER_JOBS = [
  { id: 1, title: "[Internship title]", employer: "[Employer]", deadline: "Apply by [date]" },
  { id: 2, title: "[Full-time role]", employer: "[Employer]", deadline: "Apply by [date]" },
  { id: 3, title: "[On-campus job]", employer: "[Office]", deadline: "Apply by [date]" },
];

export default function JobsPanel({ jobsHref = "#" }) {
  return (
    <section className="side-panel" aria-labelledby="jobs-heading">
      <span className="side-panel__eyebrow">Jobs &amp; internships</span>
      <h2 id="jobs-heading" className="side-panel__title">Apply soon</h2>

      <ul className="side-panel__list">
        {PLACEHOLDER_JOBS.map((job) => (
          <li key={job.id}>
            <a className="job-row" href={jobsHref}>
              <span className="job-row__title">{job.title}</span>
              <span className="job-row__employer">{job.employer}</span>
              <span className="job-row__deadline">{job.deadline}</span>
            </a>
          </li>
        ))}
      </ul>

      <a className="side-panel__button" href={jobsHref}>See all jobs &amp; internships</a>
    </section>
  );
}
