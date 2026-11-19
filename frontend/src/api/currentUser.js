/**
 * currentUser.js: who is using the site, for deciding which buttons to SHOW.
 *
 * Until the frontend has Shared Core's real login, the user comes from the dev token
 * the app was started with:
 *   npm run dev                              student (the default)
 *   VITE_DEV_TOKEN=staff-token npm run dev   Career Services staff
 *   VITE_DEV_TOKEN=admin-token npm run dev   admin
 *
 * This only hides buttons and forms a user can't use. The backend still checks the
 * role on every request (AuthInterceptor), so hiding something is never the only
 * protection. LATER: read the roles from the logged-in user instead.
 */
const AUTH_TOKEN = import.meta.env.VITE_DEV_TOKEN || "student-token";

/** True for staff and admin: they may create events and post jobs. */
export const IS_STAFF = ["staff-token", "admin-token"].includes(AUTH_TOKEN);

