/**
 * ResourceCard: one resource in the grid.
 *
 * Props:
 *   resource = { title, description, type, industry, link, majors, sharedBy }
 *
 * "Shared by" shows who posted it (the ticket asks students to see that).
 * The avatar is green for Career Services and gold for student submissions.
 * The Open link opens in a new tab; rel="noreferrer" is the safe default for that.
 */
import "./ResourceCard.css";

export default function ResourceCard({ resource }) {
  const fromStaff = resource.sharedBy === "Career Services";
  const initials = fromStaff ? "CS" : "ST";

  return (
    <article className="resource-card">
      <div className="resource-card__top">
        <span className="resource-card__type">{resource.type}</span>
        <span className="resource-card__industry">{resource.industry}</span>
      </div>

      <h3 className="resource-card__title">{resource.title}</h3>
      <p className="resource-card__desc">{resource.description}</p>

      <ul className="resource-card__majors" aria-label="For">
        {resource.majors.map((major) => (
          <li key={major}>{major}</li>
        ))}
      </ul>

      <div className="resource-card__footer">
        <div className="resource-card__shared">
          <span aria-hidden="true" className={fromStaff ? "avatar avatar--staff" : "avatar avatar--student"}>
            {initials}
          </span>
          <div>
            <span className="resource-card__label">Shared by</span>
            <span className="resource-card__by">{resource.sharedBy}</span>
          </div>
        </div>
        <a className="resource-card__open" href={resource.link} target="_blank" rel="noreferrer" aria-label={`Open ${resource.title}`}>
          Open
          <svg aria-hidden="true" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M7 17 17 7M9 7h8v8" /></svg>
        </a>
      </div>
    </article>
  );
}
