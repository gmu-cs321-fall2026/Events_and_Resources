/**
 * NavBar (MASON-50 Website navigation): the bar at the top of every page.
 *
 * Shows the site name (a link back to the home page) and one link per page in PAGES.
 * The link for the page you're on is highlighted and marked aria-current="page",
 * so screen readers say "current page" (Nielsen #1: visibility of system status).
 *
 * Props:
 *   current = the id of the page being shown, e.g. "jobs"
 */
import { PAGES } from "../utils/hashRoute";
import gmuLogo from "../assets/gmu-logo.png";
import "./NavBar.css";

export default function NavBar({ current }) {
	return (
		<header className="site-header">
			<a className="site-header__brand" href={`#${PAGES[0].id}`}>
				<img src={gmuLogo} alt="" className="site-header__logo" />
				<span>Mason CareerLaunch</span>
			</a>

			<nav aria-label="Main">
				<ul className="site-nav">
					{PAGES.map((p) => (
						<li key={p.id}>
							<a
								href={`#${p.id}`}
								className={p.id === current ? "site-nav__link site-nav__link--on" : "site-nav__link"}
								aria-current={p.id === current ? "page" : undefined}
							>
								{p.label}
							</a>
						</li>
					))}
				</ul>
			</nav>
		</header>
	);
}