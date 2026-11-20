/**
 * MyResourcesPanel (MASON-7): "My submissions", the resources I shared and where each one is.
 *
 *   Pending review -> Edit and Delete buttons (only now)
 *   Posted         -> locked, it's in the directory
 *   Denied         -> locked, with Career Services' note if they left one
 *
 * The buttons are only a convenience: the server refuses edits and deletes once a
 * resource is reviewed (409 NOT_PENDING), even if this page is out of date.
 *
 * Props:
 *   reloadKey = changes when something outside changed my list (I shared a new one)
 *   onChanged = called after I edit or delete, so the directory can reload too
 *
 * State:
 *   result      = the last answer from the server ({ key, items, error })
 *   ownKey      = goes up after my own edit/delete, so the list loads again
 *   editing     = the resource open in the edit pop-up, or null
 *   confirmId   = the resource asking "delete this?", or null
 */
import { useEffect, useState } from "react";
import { STATUS_LABELS, deleteResource, formatDate, getMyResources } from "../api/resourcesApi";
import ShareResourceModal from "./ShareResourceModal";
import "./ResourceSubmissions.css";

export default function MyResourcesPanel({ reloadKey = 0, onChanged }) {
  const [ownKey, setOwnKey] = useState(0);
  const loadKey = `${reloadKey}|${ownKey}`;
  const [result, setResult] = useState({ key: null, items: [], error: null });
  const [editing, setEditing] = useState(null);
  const [confirmId, setConfirmId] = useState(null);
  const [actionError, setActionError] = useState(null);

  /** Loads my submissions when the panel opens and after any change. */
  useEffect(() => {
    let ignore = false;
    getMyResources()
      .then((items) => {
        if (!ignore) setResult({ key: loadKey, items, error: null });
      })
      .catch((err) => {
        if (!ignore) setResult({ key: loadKey, items: [], error: err.message });
      });
    return () => {
      ignore = true;
    };
  }, [loadKey]);

  function changed() {
    setOwnKey((n) => n + 1);
    onChanged?.();
  }

  async function handleDelete(id) {
    setActionError(null);
    try {
      await deleteResource(id);
    } catch (err) {
      setActionError(err.code === "NOT_PENDING" ? "That resource was already reviewed, so it can't be deleted." : err.message);
    }
    setConfirmId(null);
    changed();
  }

  const loading = result.key !== loadKey;
  const items = result.items;

  return (
    <section className="submissions" aria-labelledby="my-submissions-title">
      <div className="submissions__head">
        <div>
          <span className="submissions__eyebrow">Shared by you</span>
          <h2 id="my-submissions-title">My submissions</h2>
        </div>
        <span aria-live="polite">{loading ? "Loading…" : `${items.length} shared`}</span>
      </div>

      {result.error && <p className="share-error" role="alert">Couldn't load your submissions: {result.error}</p>}
      {actionError && <p className="share-error" role="alert">{actionError}</p>}

      {!loading && !result.error && items.length === 0 && (
        <p className="submissions__empty">You haven't shared a resource yet. Use "Share a resource" above.</p>
      )}

      <ul className="submission-list">
        {items.map((r) => (
          <li key={r.id} className="submission">
            <div className="submission__main">
              <span className={`status-badge status-badge--${r.status}`}>{STATUS_LABELS[r.status] ?? r.status}</span>
              <h3>{r.title}</h3>
              <p className="submission__meta">
                {r.type} · for {r.majors.join(", ")} · shared {formatDate(r.submittedAt)}
              </p>
              {r.reviewNote && (
                <p className="submission__note">
                  <strong>Note from Career Services:</strong> {r.reviewNote}
                </p>
              )}
            </div>

            {r.status === "pending" && confirmId !== r.id && (
              <div className="submission__actions">
                <button type="button" className="sub-button sub-button--plain" onClick={() => setEditing(r)}>Edit</button>
                <button type="button" className="sub-button sub-button--danger" onClick={() => setConfirmId(r.id)}>Delete</button>
              </div>
            )}

            {confirmId === r.id && (
              <div className="submission__actions" role="alert">
                <span>Delete it? This can't be undone.</span>
                <button type="button" className="sub-button sub-button--plain" onClick={() => setConfirmId(null)}>Keep it</button>
                <button type="button" className="sub-button sub-button--danger" onClick={() => handleDelete(r.id)}>Yes, delete</button>
              </div>
            )}

            {r.status !== "pending" && (
              <p className="submission__locked">
                {r.status === "approved" ? "Posted in the directory, so it can't be changed." : "Reviewed, so it can't be changed."}
              </p>
            )}
          </li>
        ))}
      </ul>

      {editing && (
        <ShareResourceModal
          resource={editing}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            changed();
          }}
        />
      )}
    </section>
  );
}
