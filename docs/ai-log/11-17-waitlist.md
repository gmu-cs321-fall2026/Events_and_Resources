# AI Log: MASON-18 Be waitlisted + MASON-19 Review event waitlist

## Asked
Finish MASON-18 and MASON-19 today. Example to support: an event has 30 seats and 30 registered, a 31st student goes on the waitlist, and when admin raises the capacity (e.g. 30 -> 35) the waiting student is accepted automatically. Admin must be able to see the waitlist and increase the capacity. (Team decision, Nov 16: "override" = admin raises the capacity; nobody goes over capacity.)

## Produced
**Backend**
- `schema.sql`: new `waitlist` table (event_id, user_id, created_at, UNIQUE per event+user). Line order = id order.
- `model/EventSeats.java`, `WaitlistEntry.java`, `WaitlistView.java`, `CapacityRequest.java` (new records).
- `model/RegistrationStatus.java`: added `waitlisted` and `waitlistPosition`, so `GET /events/{id}/register` tells a student their place in line.
- `repository/WaitlistRepository.java` (new): the SQL. `lockSeats` uses `SELECT ... FOR UPDATE`; `moveToRegistrations` deletes the waitlist row, inserts the registration and adds 1 to registered_count in one statement.
- `service/WaitlistService.java` (new): join / leave / staff view / changeCapacity, plus `fillOpenSeats`, which gives every open seat to the next person in line.
- `controller/WaitlistController.java` (new): `POST`/`DELETE /events/{id}/waitlist`, `GET /events/{id}/waitlist` (staff), `PUT /events/{id}/capacity` (staff).
- `service/RegistrationService.java`: cancelling now calls `fillOpenSeats`, so a freed seat goes to the waitlist; `status` includes the waitlist position.
- `common/AuthInterceptor.java`: the waitlist URLs need a login; seeing the waitlist and changing the capacity need staff/admin.
- Tests: `WaitlistServiceTest.java` (new, 16 tests, including the 30 -> 31st student -> 35 example), 4 new tests in `RegistrationServiceCancelTest.java`, 5 new tests in `AuthInterceptorTest.java`.

**Frontend**
- `api/waitlistApi.js` (new): joinWaitlist, leaveWaitlist, getWaitlist, changeCapacity.
- `components/RegisterButton.jsx`: "Join waitlist" when the event is full, "On the waitlist · #2" + "Leave waitlist" while waiting.
- `components/WaitlistPanel.jsx` + `.css` (new): staff box in the event pop-up that lists the line and has the capacity form.
- `components/EventDetailModal.jsx`: shows WaitlistPanel for staff (IS_STAFF) on limited events.

## How it was checked
- Every new SQL statement was run by hand against PostgreSQL 16 with our schema: 30/30 event, two students waiting, positions 1 and 2, capacity 29 refused by the CHECK rule, capacity 31 moved exactly the first student in (31 registered, 1 still waiting).
- The Java service and auth tests were compiled and run (50 passing). Still to do on our machines: run `./mvnw test` and click through the pop-up as student-token and staff-token.

## Changed or rejected
- Rejected letting staff push one person past capacity ("override"): the team decided override = raise the capacity, and the database CHECK rule already blocks going over.
- Rejected a capacity lower than the people already registered (400): that would mean kicking out registered students.
- Rejected making students click "accept" when a spot opens: the ticket says they get the spot, so they're registered automatically and the pop-up / My events calendar shows it.
- Fill happens on cancel too, not only when capacity goes up, since "wait for a vacant spot" is in MASON-18's acceptance criteria.
- `GET /events/{id}/waitlist` isn't paginated: one event's waitlist is short, and staff need the whole line in order.
- Notifications ("You got a spot") are left as a TODO in `fillOpenSeats` until the MASON-24 Notifications client exists.
- Mock mode (`VITE_USE_MOCK=true`) doesn't support the waitlist; the buttons show a message saying it needs the real backend.
