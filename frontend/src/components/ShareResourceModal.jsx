/**
 * ShareResourceModal (MASON-7): the pop-up form to share a resource, or edit one that's
 * still pending.
 *
 * Students: what they share is "Pending review" until Career Services approves it.
 * Staff/admin: what they share is posted right away (the server decides, not this form).
 *
 * Props:
 *   resource = the resource to edit, or nothing to share a new one
 *   onSaved  = called with the saved resource after the server says OK
 *   onClose  = closes the pop-up (Cancel, ×, Escape, or a click outside)
 *
 * State:
 *   form   = what's typed in every box right now; majors is a list of picked audiences
 *   error  = the server's message if saving failed (e.g. "title: required")
 *   saving = true while waiting for the server, so the button can't be pressed twice
 */
import { useEffect, useState } from "react";
import { AUDIENCES, SHAREABLE_TYPES, submitResource, updateResource } from "../api/resourcesApi";
import { useAuth } from "../auth/useAuth";
import "./EventDetailModal.css";
import "./ResourceSubmissions.css";

const EMPTY_FORM = { title: "", type: "", industry: "", link: "", description: "", majors: [] };

export default function ShareResourceModal({ resource, onSaved, onClose }) {
  const { isStaff } = useAuth();
  const editing = Boolean(resource);

  /** Starting values: the resource's own values when editing, blank boxes when sharing. */
  const [form, setForm] = useState(() =>
    editing
      ? {
          title: resource.title,
          type: resource.type,
          industry: resource.industry === "All industries" ? "" : resource.industry,
          link: resource.link ?? "",
          description: resource.description,
          majors: resource.majors,
        }
      : EMPTY_FORM
  );
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  /** Escape closes the pop-up. The cleanup removes the listener when it closes. */
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === "Escape") onClose();
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  }

  /** Ticks or unticks one audience. A new array is made so React sees the change. */
  function toggleMajor(name) {
    const majors = form.majors.includes(name)
      ? form.majors.filter((m) => m !== name)
      : [...form.majors, name];
    setForm({ ...form, majors });
  }

  /**
   * Sends the resource. The backend checks every field again, so its message is shown
   * if anything is wrong (for example 409 if staff reviewed it while the form was open).
   */
  async function handleSubmit(e) {
    e.preventDefault();
    if (form.majors.length === 0) {
      setError("Pick at least one major or class year (or All Students).");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const saved = editing ? await updateResource(resource.id, form) : await submitResource(form);
      onSaved(saved);
    } catch (err) {
      setError(err.code === "NOT_PENDING" ? "This resource was already reviewed, so it can't be edited anymore." : err.message);
      setSaving(false);
    }
  }

  const hint = isStaff
    ? "Posts from Career Services staff appear in the directory right away."
    : "Career Services reviews student resources before they're posted. You can edit or delete yours until then.";

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="modal share-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="share-form-title"
        onClick={(e) => e.stopPropagation()}
      >
        <button type="button" className="modal-close" onClick={onClose} aria-label="Close">×</button>

        <form className="share-form" onSubmit={handleSubmit}>
          <h2 id="share-form-title">{editing ? "Edit your resource" : "Share a resource"}</h2>
          <p className="share-form__hint">{hint}</p>

          <label>
            Title
            <input name="title" value={form.title} onChange={handleChange} maxLength={200}
              placeholder="Questions I was asked at the fall career fair" required autoFocus />
          </label>

          <div className="share-form__row">
            <label>
              Type
              <select name="type" value={form.type} onChange={handleChange} required>
                <option value="" disabled>Choose a type</option>
                {SHAREABLE_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </label>
            <label>
              <span>Industry <span className="share-form__optional">(optional)</span></span>
              <input name="industry" value={form.industry} onChange={handleChange} maxLength={100}
                placeholder="Technology" />
            </label>
          </div>

          <label>
            Description
            <textarea name="description" value={form.description} onChange={handleChange} maxLength={2000}
              rows={5} placeholder="What's in it and who it helps. Tips and your own experience are welcome." required />
          </label>

          <label>
            <span>Link <span className="share-form__optional">(optional)</span></span>
            <input type="url" name="link" value={form.link} onChange={handleChange} maxLength={500}
              placeholder="https://example.com/guide" />
          </label>

          <fieldset className="share-form__majors">
            <legend>Who is it for?</legend>
            <div className="share-chips">
              {AUDIENCES.map((name) => (
                <button
                  key={name}
                  type="button"
                  className={form.majors.includes(name) ? "share-chip share-chip--on" : "share-chip"}
                  aria-pressed={form.majors.includes(name)}
                  onClick={() => toggleMajor(name)}
                >
                  {name}
                </button>
              ))}
            </div>
          </fieldset>

          {error && <p className="share-error" role="alert">Could not save: {error}</p>}

          <div className="share-form__actions">
            <button type="button" className="sub-button sub-button--plain" onClick={onClose}>Cancel</button>
            <button type="submit" className="sub-button" disabled={saving}>
              {saving ? "Saving…" : editing ? "Save changes" : isStaff ? "Post" : "Submit for review"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
