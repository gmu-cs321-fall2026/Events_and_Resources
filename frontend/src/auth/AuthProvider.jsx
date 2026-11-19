/**
 * AuthProvider: holds "who is logged in" for the whole website.
 *
 * main.jsx wraps <App /> in it, so every component can call useAuth().
 * The token itself lives in api/auth.js; this component keeps the USER in React
 * state, because changing state is what makes React redraw the page (show the
 * Create Event form after a staff login, hide it after logging out).
 *
 * STATE
 *   user   = { userId, roles, staff, email } or null
 *   notice = a message for the login form ("Your login expired. Please log in again.")
 */
import { useEffect, useState } from "react";
import { getMe, getToken, isStaff, login, logout, setOnExpired } from "../api/auth";
import { AuthContext } from "./useAuth";

export default function AuthProvider({ children }) {
	const [user, setUser] = useState(null);
	const [notice, setNotice] = useState("");

	/**
	 * Runs once, when the website starts ([] = no dependencies).
	 *  1. Tell api/auth.js what to do when a token stops working (a 401):
	 *     forget the user and ask them to log in again.
	 *  2. If we start with a token (VITE_DEV_TOKEN), ask the backend who it belongs to.
	 * The return function undoes step 1 if this component is ever removed.
	 */
	useEffect(() => {
		setOnExpired(() => {
			setUser(null);
			setNotice("Your login expired. Please log in again.");
		});

		if (getToken()) {
			getMe()
				.then((me) => setUser({ ...me, email: null }))
				.catch(() => logout());
		}

		return () => setOnExpired(null);
	}, []);

	/** The login form calls this. Errors are passed on so the form can show them. */
	async function logIn(email, password) {
		const me = await login(email, password);
		setUser({ ...me, email });
		setNotice("");
	}

	function logOut() {
		logout();
		setUser(null);
		setNotice("");
	}

	const value = { user, isStaff: isStaff(user), notice, logIn, logOut };

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}