/**
 * useAuth: lets any component ask "who is logged in?".
 *
 *   const { user, isStaff, logIn, logOut, notice } = useAuth();
 *
 *   user     = { userId, roles, staff, email } or null when logged out
 *   isStaff  = true for staff or admin (show Create Event, job and approval buttons)
 *   logIn    = async (email, password) => throws an Error with a message if it fails
 *   logOut   = () => forgets the token
 *   notice   = a message for the login form, e.g. "Your login expired..."
 *
 * HOW IT WORKS: React "context" is a value that a parent component (AuthProvider)
 * makes available to every component inside it, without passing props down by hand.
 * useContext(AuthContext) reads it. When AuthProvider's state changes (someone logs
 * in), every component using useAuth() re-renders with the new value.
 *
 * This file only holds the context and the hook; AuthProvider.jsx holds the component.
 * (React's fast refresh works best when a .jsx file only exports components.)
 */
import { createContext, useContext } from "react";

export const AuthContext = createContext(null);

export function useAuth() {
	const value = useContext(AuthContext);
	if (!value) {
		throw new Error("useAuth() must be used inside <AuthProvider> (see main.jsx)");
	}
	return value;
}
