-- Sample events for testing. Spring Boot runs this file at startup, after schema.sql.
-- Each INSERT only runs if an event with that title doesn't exist yet,
-- so restarting the app doesn't create duplicates.

-- categories and audiences go in first, then events,
-- then event_audiences (which needs both an event id and an audience id).
 
-- CATEGORIES
-- "ON CONFLICT (name) DO NOTHING": name is UNIQUE, so if a category with that
-- name already exists, Postgres skips that row instead of throwing an error.
INSERT INTO categories (name, description) VALUES
    ('Internal',              'Staff-only events run by Career Services'),
    ('Career Fair',           'Many employers recruiting in one place'),
    ('Workshop',              'Hands-on session to build a career skill'),
    ('Employer Info Session', 'One company presents its roles and culture'),
    ('Networking',            'Meet professionals, alumni and peers'),
    ('Panel',                 'Several speakers answer questions on a topic')
ON CONFLICT (name) DO NOTHING;
 
-- AUDIENCES
INSERT INTO audiences (name, description) VALUES
    ('All Students',              'Open to every enrolled student'),
    ('Freshmen',                  'First-year undergraduates'),
    ('Sophomores',                'Second-year undergraduates'),
    ('Juniors',                   'Third-year undergraduates'),
    ('Seniors',                   'Fourth-year undergraduates'),
    ('Graduate Students',         'Master''s and PhD students'),
    ('Alumni',                    'Graduates of the university'),
    ('CS Majors',                 'Computer Science majors'),
    ('IT Majors',                 'Information Technology majors'),
    ('International Students',    'Students on a student visa'),
    ('First-Generation Students', 'First in their family to attend college'),
    ('Staff',                     'Career Services and university staff')
ON CONFLICT (name) DO NOTHING;
 
-- EVENTS
-- events has no UNIQUE column besides id, so ON CONFLICT can't help here.
-- Instead each INSERT ... SELECT ... WHERE NOT EXISTS only adds the event
-- if no event with that title is there yet.
-- Dates run from Oct to Dec 2026, with a few on the same day, so the
-- calendar's day, week and month views all have something to show.
-- The audience column is plain text for display; event_audiences (section 4)
-- holds the same audiences as real links.

-- Unlimited capacity (0): anyone can register.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Fall Career Fair', 'Career Services', '2026-10-28', '10:00', 'EagleBank Arena', 'Career Fair', 'All Students', 'In-person', 0
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Fall Career Fair');

-- Limited capacity: 30 seats.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Resume Workshop', 'Career Services', '2026-10-20', '15:00', 'Horizon Hall 2400', 'Workshop', 'All Students', 'In-person', 30
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Resume Workshop');

-- ONE seat: use this to test "event full". The first student gets it, the second gets 409 EVENT_FULL.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Capital One Tech Talk', 'Capital One', '2026-10-22', '17:30', 'Johnson Center', 'Employer Info Session', 'All Students', 'Virtual', 1
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Capital One Tech Talk');

INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'LinkedIn Profile Lab', 'Career Services', '2026-10-27', '12:00', 'Johnson Center 239', 'Workshop', 'Freshmen, Sophomores', 'In-person', 25
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'LinkedIn Profile Lab');
 
-- Same day as the Fall Career Fair: tests two events on one calendar day.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Women in Tech Networking Night', 'Women in Computing', '2026-10-28', '18:00', 'Horizon Hall 1010', 'Networking', 'CS Majors, IT Majors', 'In-person', 60
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Women in Tech Networking Night');
 
-- Has a register_link (the optional column), so we list it in the column list too.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, register_link, capacity)
SELECT 'Deloitte Info Session', 'Deloitte', '2026-10-29', '17:00', 'Zoom', 'Employer Info Session', 'Juniors, Seniors', 'Virtual', 'https://example.com/register/deloitte-info-session', 0
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Deloitte Info Session');
 
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Mock Technical Interviews', 'CS Department', '2026-11-03', '14:00', 'Nguyen Engineering 1103', 'Workshop', 'CS Majors, Juniors, Seniors', 'In-person', 12
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Mock Technical Interviews');

-- 4. EVENT_AUDIENCES (links each event to its audiences)
-- We don't know the ids ahead of time (the database picks them), so we look
-- them up by event title and audience name:
--   - VALUES (...) builds a small temporary table of (title, audience name) pairs
--   - JOIN events    matches each title to its event id
--   - JOIN audiences matches each audience name to its audience id
-- ON CONFLICT DO NOTHING skips pairs that are already linked (the primary key
-- is (event_id, audience_id), so a repeat would otherwise be an error).
INSERT INTO event_audiences (event_id, audience_id)
SELECT e.id, a.id
FROM (VALUES
    ('Fall Career Fair',                            'All Students'),
    ('Resume Workshop',                             'All Students'),
    ('Capital One Tech Talk',                       'All Students'),
    ('LinkedIn Profile Lab',                        'Freshmen'),
    ('LinkedIn Profile Lab',                        'Sophomores'),
    ('Women in Tech Networking Night',              'CS Majors'),
    ('Women in Tech Networking Night',              'IT Majors'),
    ('Deloitte Info Session',                       'Juniors'),
    ('Deloitte Info Session',                       'Seniors'),
    ('Mock Technical Interviews',                   'CS Majors'),
    ('Mock Technical Interviews',                   'Juniors'),
    ('Mock Technical Interviews',                   'Seniors'),
    ('Grad School Panel: Is a Master''s Worth It?', 'Juniors'),
    ('Grad School Panel: Is a Master''s Worth It?', 'Seniors'),
    ('Grad School Panel: Is a Master''s Worth It?', 'International Students'),
    ('Federal Careers Info Session',                'All Students'),
    ('Spring Internship Fair',                      'All Students'),
    ('First-Gen Career Mixer',                      'First-Generation Students'),
    ('Salary Negotiation 101',                      'Seniors'),
    ('Salary Negotiation 101',                      'Graduate Students'),
    ('Alumni in Industry Panel',                    'All Students'),
    ('Alumni in Industry Panel',                    'Alumni'),
    ('Career Services Staff Training',              'Staff'),
    ('Cybersecurity Careers Networking Hour',       'CS Majors'),
    ('Cybersecurity Careers Networking Hour',       'IT Majors')
) AS link(event_title, audience_name)
JOIN events   e ON e.event_title = link.event_title
JOIN audiences a ON a.name        = link.audience_name
ON CONFLICT DO NOTHING;
