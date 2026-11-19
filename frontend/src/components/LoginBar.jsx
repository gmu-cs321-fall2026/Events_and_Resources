/**
 * LoginBar: the strip at the top of the website.
 *
 *   Logged out: a "Log in" button that opens a small email + password form.
 *   Logged in:  "Logged in as staff@gmu.edu (staff)" and a "Log out" button.
 *
 * Guests can still browse events, resources and jobs; logging in is only needed to
 * register for events, or (as staff/admin) to create events, post jobs and approve resources.
 *
 * STATE (only this component needs it, so it lives here, not in AuthProvider)
 *   open      = is the form showing?
 *   email, password = what's typed in the boxes ("controlled inputs": React state is
 *                     the single source of truth, and onChange copies each keystroke into it)
 *   busy      = a login request is on its way (blocks double submits)
 *   error     = why the last attempt failed, shown under the form
 */
import { useState } from "react";
import { useAuth } from "../auth/useAuth";
import "./LoginBar.css";

export default function LoginBar() {
	const { user, notice, logIn, logOut } = useAuth();
	const [open, setOpen] = useState(false);
	const [email, setEmail] = useState("");
	const [password, setPassword] = useState("");
	const [busy, setBusy] = useState(false);
	const [error, setError] = useState("");

	/**
	 * Runs when the form is submitted (button click or Enter).
	 * preventDefault() stops the browser's own form submit, which would reload the page.
	 * The password box is cleared either way, so it never sits on screen.
	 */
	async function handleSubmit(e) {
		e.preventDefault();
		setBusy(true);
		setError("");
		try {
			await logIn(email.trim(), password);
			setOpen(false);
			setEmail("");
		} catch (err) {
			setError(err.message);
		} finally {
			setPassword("");
			setBusy(false);
		}
	}

	if (user) {
		return (
			<div className="login-bar">
				<span>
					Logged in as <strong>{user.email || "test user"}</strong> ({user.roles.join(", ")})
				</span>
				<button type="button" className="login-bar-button" onClick={logOut}>
					Log out
				</button>
			</div>
		);
	}

	/** A notice (like "your login expired") opens the form on its own. */
	const showForm = open || Boolean(notice);

	return (
		<div className="login-bar">
			{!showForm && (
				<button type="button" className="login-bar-button" onClick={() => setOpen(true)}>
					Log in
				</button>
			)}

			{showForm && (
				<form className="login-form" onSubmit={handleSubmit}>
					{notice && <p className="login-notice" role="status">{notice}</p>}

					<label>
						Email
						<input
							type="email"
							value={email}
							onChange={(e) => setEmail(e.target.value)}
							autoComplete="username"
							required
						/>
					</label>

					<label>
						Password
						<input
							type="password"
							value={password}
							onChange={(e) => setPassword(e.target.value)}
							autoComplete="current-password"
							required
						/>
					</label>

					<button type="submit" className="login-bar-button" disabled={busy}>
						{busy ? "Logging in…" : "Log in"}
					</button>
					{!notice && (
						<button type="button" className="login-cancel" onClick={() => setOpen(false)} disabled={busy}>
							Cancel
						</button>
					)}

					{error && <p className="login-error" role="alert">{error}</p>}
				</form>
			)}
		</div>
	);
}
