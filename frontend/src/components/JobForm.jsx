/**
 * JobForm (MASON-13): the staff form to post a new job or edit an existing one.
 *
 * Props:
 *   job     = the posting to edit, or nothing to post a new one
 *   onSaved = called with the saved posting after the server says OK
 *   onCancel = called when staff press Cancel
 *
 * State:
 *   form   = what's typed in every box right now
 *   error  = the server's message if saving failed (e.g. "deadline: can't be in the past")
 *   saving = true while waiting for the server, so the button can't be pressed twice
 */
import { useState } from "react";
import { JOB_TYPES, createOpportunity, updateOpportunity } from "../api/opportunitiesApi";
import "../pages/JobsPage.css";

const EMPTY_FORM = { title: "", employer: "", type: "", deadline: "", link: "", description: "" };

export default function JobForm({ job, onSaved, onCancel }) {
	const editing = Boolean(job);

	/**
	 * Starting values: the posting's own values when editing, blank boxes when posting.
	 * "?? """ turns a missing value into an empty box instead of "undefined".
	 */
	const [form, setForm] = useState(() =>
		editing
			? Object.fromEntries(Object.keys(EMPTY_FORM).map((k) => [k, job[k] ?? ""]))
			: EMPTY_FORM
	);
	const [error, setError] = useState(null);
	const [saving, setSaving] = useState(false);

	function handleChange(e) {
		const { name, value } = e.target;
		setForm({ ...form, [name]: value });
	}

	/**
	 * Sends the posting to the server. The backend checks every field again,
	 * so its message is shown if anything is wrong.
	 */
	async function handleSubmit(e) {
		e.preventDefault();
		setSaving(true);
		setError(null);

		const posting = {
			title: form.title.trim(),
			employer: form.employer.trim(),
			type: form.type,
			deadline: form.deadline,
			link: form.link.trim(),
			description: form.description.trim(),
		};

		try {
			const saved = editing ? await updateOpportunity(job.id, posting) : await createOpportunity(posting);
			onSaved(saved);
		} catch (err) {
			setError(err.message);
			setSaving(false);
		}
	}

	/** Today as YYYY-MM-DD: the date picker won't let staff pick an earlier deadline for a new post. */
	const today = new Date().toLocaleDateString("en-CA");

	return (
		<form className="job-form" onSubmit={handleSubmit}>
			<h2 id="job-form-title">{editing ? "Edit posting" : "Post a job or internship"}</h2>

			<label>
				Title
				<input name="title" value={form.title} onChange={handleChange} maxLength={200}
					placeholder="Software Engineering Intern, Summer 2027" required />
			</label>

			<label>
				Employer
				<input name="employer" value={form.employer} onChange={handleChange} maxLength={200}
					placeholder="Capital One" required />
			</label>

			<div className="job-form__row">
				<label>
					Type
					<select name="type" value={form.type} onChange={handleChange} required>
						<option value="" disabled>Choose a type</option>
						{JOB_TYPES.map((t) => (
							<option key={t} value={t}>{t}</option>
						))}
					</select>
				</label>
				<label>
					Application deadline
					<input type="date" name="deadline" value={form.deadline} onChange={handleChange}
						min={editing ? undefined : today} required />
				</label>
			</div>

			<label>
				Application link
				<input type="url" name="link" value={form.link} onChange={handleChange} maxLength={500}
					placeholder="https://careers.example.com/apply" required />
			</label>

			<label>
				Description
				<textarea name="description" value={form.description} onChange={handleChange} maxLength={2000}
					rows={5} placeholder="What the role involves, who should apply, and where it's located." required />
			</label>

			{error && <p className="job-error" role="alert">Could not save: {error}</p>}

			<div className="job-form__actions">
				<button type="button" className="job-button job-button--plain" onClick={onCancel}>Cancel</button>
				<button type="submit" className="job-button" disabled={saving}>
					{saving ? "Saving…" : editing ? "Save changes" : "Post"}
				</button>
			</div>
		</form>
	);
}