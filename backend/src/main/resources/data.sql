-- Sample events for testing. Spring Boot runs this file at startup, after schema.sql.
-- Each INSERT only runs if an event with that title doesn't exist yet,
-- so restarting the app doesn't create duplicates.

-- Unlimited capacity (0): anyone can register.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Fall Career Fair', 'Career Services', '2026-10-28', '10:00', 'EagleBank Arena', 'Career Fair', 0
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Fall Career Fair');

-- Limited capacity: 30 seats.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Resume Workshop', 'Career Services', '2026-10-20', '15:00', 'Horizon Hall 2400', 'Workshop', 30
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Resume Workshop');

-- ONE seat: use this to test "event full". The first student gets it, the second gets 409 EVENT_FULL.
INSERT INTO events (event_title, host, event_date, event_time, location, category, audience, format, capacity)
SELECT 'Capital One Tech Talk', 'Capital One', '2026-10-22', '17:30', 'Johnson Center', 'Employer Info Session', 1
WHERE NOT EXISTS (SELECT 1 FROM events WHERE event_title = 'Capital One Tech Talk');
