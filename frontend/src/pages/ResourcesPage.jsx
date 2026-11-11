/**
 * ResourcesPage (MASON-12 Browse resources directory).
 *
 * A student picks a major and/or types a keyword and sees matching prep resources,
 * each showing who shared it. Beside it: upcoming events for that major (real data)
 * and a jobs panel (placeholder until MASON-13).
 *
 * This page has NO top navigation of its own: Cindy's MASON-50 navigation mounts it
 * as the "Resources" tab.
 *
 * Props (all optional, filled in as teammates push):
 *   onShare       = opens Jean's "submit a resource" form (MASON-7). Without it the
 *                   button is disabled and says "coming soon".
 *   calendarHref  = link to Trang's calendar (MASON-3)
 *   jobsHref      = link to the Jobs page (MASON-13), "#jobs" by default
 *   jobsReloadKey = changes after staff post/edit/delete a job, so the jobs panel reloads
 *
 * State:
 *   major, type, query = the filters. Changing any one re-runs the useEffect,
 *   which asks getResources() for the matching list.
 */
import { useEffect, useState } from "react";
import { MAJORS, RESOURCE_TYPES, getResources } from "../api/resourcesApi";
import ResourceCard from "../components/ResourceCard";
import UpcomingEventsPanel from "../components/UpcomingEventsPanel";
import JobsPanel from "../components/JobsPanel";
import gmuLogo from "../assets/gmu-logo.png";
import "./ResourcesPage.css";

export default function ResourcesPage({ onShare, calendarHref = "#", jobsHref = "#jobs", jobsReloadKey = 0 }) {
  const [major, setMajor] = useState("All majors");
  const [type, setType] = useState("All types");
  const [query, setQuery] = useState("");

  const filterKey = `${major}|${type}|${query}`;
  const [result, setResult] = useState({ key: null, items: [], error: null });

  useEffect(() => {
    let ignore = false;
    getResources({ q: query, major, type })
      .then((items) => {
        if (!ignore) setResult({ key: filterKey, items, error: null });
      })
      .catch((err) => {
        if (!ignore) setResult({ key: filterKey, items: [], error: err.message });
      });
    return () => {
      ignore = true;
    };
  }, [filterKey, major, type, query]);

  const loading = result.key !== filterKey;
  const resources = result.items;
  const error = result.error;
  function clearFilters() {
    setMajor("All majors");
    setType("All types");
    setQuery("");
  }

  const heading = major === "All majors" ? "All resources" : `Resources for ${major}`;
  const count = `${resources.length} ${resources.length === 1 ? "resource" : "resources"}`;

  return (
    <div className="resources-page">
      <section className="resources-hero">
        <div className="hero-logo">
          <img src={gmuLogo} alt="George Mason University" />
        </div>
        <div className="resources-hero__top">
          <span className="resources-hero__eyebrow">Career Resources</span>
          <button
            type="button"
            className="share-button"
            onClick={onShare}
            disabled={!onShare}
            title={onShare ? undefined : "Coming soon"}
          >
            <svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><path d="M12 5v14M5 12h14" /></svg>
            Share a resource
          </button>
        </div>

        <h1 className="resources-hero__title">Prep for where you're headed.</h1>
        <p className="resources-hero__lead">
          Resume guides, interview question banks, salary tips and practice tools, picked by Career
          Services and shared by students. Choose your major or class year to see what fits.
        </p>

        <label className="search-box">
          <svg aria-hidden="true" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
          <span className="visually-hidden">Search resources</span>
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder='Search by keyword, industry or role (e.g. "software")'
          />
        </label>

        <div className="chip-group">
          <span id="major-label" className="chip-group__label">Browse by major or class year</span>
          <div role="group" aria-labelledby="major-label" className="chip-row">
            {MAJORS.map((m) => (
              <button
                key={m}
                type="button"
                className={m === major ? "chip chip--on" : "chip"}
                aria-pressed={m === major}
                onClick={() => setMajor(m)}
              >
                {m}
              </button>
            ))}
          </div>
        </div>
      </section>

      <div className="resources-body">
        <main className="resources-main">
          <div className="results-head">
            <h2>{heading}</h2>
            <span aria-live="polite">{loading ? "Loading…" : count}</span>
          </div>

          <div role="group" aria-label="Resource type" className="pill-row">
            {RESOURCE_TYPES.map((t) => (
              <button
                key={t}
                type="button"
                className={t === type ? "pill pill--on" : "pill"}
                aria-pressed={t === type}
                onClick={() => setType(t)}
              >
                {t}
              </button>
            ))}
          </div>

          {error && <p className="resources-error" role="alert">Couldn't load resources: {error}</p>}

          {!error && !loading && resources.length === 0 && (
            <div className="resources-empty">
              <h3>No resources match yet</h3>
              <p>Try another major or type, or clear your search.</p>
              <button type="button" className="clear-button" onClick={clearFilters}>Clear filters</button>
            </div>
          )}

          {!error && resources.length > 0 && (
            <div className="resource-grid">
              {resources.map((r) => (
                <ResourceCard key={r.id} resource={r} />
              ))}
            </div>
          )}
        </main>

        <aside className="resources-side">
          <UpcomingEventsPanel major={major} calendarHref={calendarHref} />
          <JobsPanel jobsHref={jobsHref} reloadKey={jobsReloadKey} />
        </aside>
      </div>
    </div>
  );
}
