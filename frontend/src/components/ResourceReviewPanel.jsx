/**
 * ResourceReviewPanel (MASON-11): the staff/admin list of student resources to review.
 *
 * Tabs pick which status to look at:
 *   Pending review = waiting (oldest first, so nobody waits forever)
 *   Posted         = approved, live in the directory
 *   Denied         = rejected
 *
 * Each resource can be moved to any other status:
 *   Approve        -> "approved" (appears in the directory)
 *   Deny           -> "rejected"
 *   Put on hold    -> "pending" (back in the queue; the student can edit it again)
 * The optional note is shown to the student in "My submissions" (e.g. why it was denied).
 *
 * App.jsx only shows this panel to staff/admin, and the server checks it again (403).
 *
 * Props:
 *   onChanged = called after a decision, so the public directory can reload
 *
 * State:
 *   status  = the open tab
 *   result  = the last answer from the server
 *   notes   = what's typed in each resource's note box, by id: { 12: "Broken link" }
 *   busyId  = the resource being saved right now (its buttons are disabled)
 */
import { useEffect, useState } from "react";
import { STATUS_LABELS, formatDate, getReviewQueue, reviewResource } from "../api/resourcesApi";
import "./ResourceSubmissions.css";

const TABS = ["pending", "approved", "rejected"];

/** The buttons each status gets: you can move a resource to any status except its own. */
const ACTIONS = [
  { status: "approved", label: "Approve", className: "sub-button" },
  { status: "rejected", label: "Deny", className: "sub-button sub-button--danger" },
  { status: "pending", label: "Put on hold", className: "sub-button sub-button--plain" },
];

export default function ResourceReviewPanel({ onChanged }) {
  const [status, setStatus] = useState("pending");
  const [reloadKey, setReloadKey] = useState(0);
  const loadKey = `${status}|${reloadKey}`;
  const [result, setResult] = useState({ key: null, items: [], error: null });
  const [notes, setNotes] = useState({});
  const [busyId, setBusyId] = useState(null);
  const [actionError, setActionError] = useState(null);

  useEffect(() => {
    let ignore = false;
    getReviewQueue(status)
      .then((items) => {
        if (!ignore) setResult({ key: loadKey, items, error: null });
      })
      .catch((err) => {
        if (!ignore) setResult({ key: loadKey, items: [], error: err.message });
      });
    return () => {
      ignore = true;
    };
  }, [loadKey, status]);

  /** Saves one decision, then reloads this tab (the resource moves to another tab). */
  async function decide(resource, newStatus) {
    setBusyId(resource.id);
    setActionError(null);
    try {
      await reviewResource(resource.id, newStatus, notes[resource.id] ?? "");
      setNotes({ ...notes, [resource.id]: "" });
      setReloadKey((n) => n + 1);
      onChanged?.();
    } catch (err) {
      setActionError(`"${resource.title}": ${err.message}`);
    }
    setBusyId(null);
  }

  const loading = result.key !== loadKey;
  const items = result.items;

  return (
    <section className="submissions submissions--staff" aria-labelledby="review-title">
      <div className="submissions__head">
        <div>
          <span className="submissions__eyebrow">Career Services</span>
          <h2 id="review-title">Review shared resources</h2>
        </div>
        <span aria-live="polite">{loading ? "Loading…" : `${items.length} ${STATUS_LABELS[status].toLowerCase()}`}</span>
      </div>

      <div role="group" aria-label="Show resources that are" className="review-tabs">
        {TABS.map((t) => (
          <button
            key={t}
            type="button"
            className={t === status ? "review-tab review-tab--on" : "review-tab"}
            aria-pressed={t === status}
            onClick={() => setStatus(t)}
          >
            {STATUS_LABELS[t]}
          </button>
        ))}
      </div>

      {result.error && <p className="share-error" role="alert">Couldn't load resources: {result.error}</p>}
      {actionError && <p className="share-error" role="alert">Could not save {actionError}</p>}

      {!loading && !result.error && items.length === 0 && (
        <p className="submissions__empty">
          {status === "pending" ? "Nothing waiting for review. Nice work." : `No ${STATUS_LABELS[status].toLowerCase()} resources.`}
        </p>
      )}

      <ul className="submission-list">
        {items.map((r) => (
          <li key={r.id} className="submission submission--review">
            <div className="submission__main">
              <span className={`status-badge status-badge--${r.status}`}>{STATUS_LABELS[r.status]}</span>
              <h3>{r.title}</h3>
              <p className="submission__meta">
                {r.type} · {r.industry} · for {r.majors.join(", ")} · {r.sharedBy} · shared {formatDate(r.submittedAt)}
              </p>
              <p className="submission__desc">{r.description}</p>
              {r.link && (
                <a className="submission__link" href={r.link} target="_blank" rel="noopener noreferrer">
                  Check the link: {r.link}
                </a>
              )}
              {r.reviewNote && <p className="submission__note"><strong>Last note:</strong> {r.reviewNote}</p>}
            </div>

            <div className="review-decision">
              <label>
                <span>Note to the student <span className="share-form__optional">(optional)</span></span>
                <textarea
                  rows={2}
                  maxLength={500}
                  value={notes[r.id] ?? ""}
                  onChange={(e) => setNotes({ ...notes, [r.id]: e.target.value })}
                  placeholder={status === "pending" ? "e.g. why it was denied" : "e.g. why it's back on hold"}
                />
              </label>
              <div className="submission__actions">
                {ACTIONS.filter((a) => a.status !== r.status).map((a) => (
                  <button
                    key={a.status}
                    type="button"
                    className={a.className}
                    disabled={busyId === r.id}
                    onClick={() => decide(r, a.status)}
                  >
                    {a.label}
                  </button>
                ))}
              </div>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
