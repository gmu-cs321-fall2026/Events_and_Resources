# AI Log: MASON-7 Submit a resource for review + MASON-11 Approve resources from students

## Asked
Complete both stories based on the current repo:
- MASON-7: a student can submit a resource, waits for admin approval and sees the status update, and can edit/delete it while it's pending but not once it's posted.
- MASON-11: an admin sees the list of student resources, can approve or deny them, and can update the status of on-hold posts.

## Produced
**Backend**
- `schema.sql`: the existing `resource` table gets the columns a card and the review queue need (title, description, resource_type, industry, created_at, updated_at, reviewed_by, reviewed_at, review_note), all `ADD COLUMN IF NOT EXISTS`, so no database reset. `user_id` becomes a UUID (Shared Core's id, like `registrations.user_id`).
- `model/Resource.java` (answer), `ResourceRequest.java` (what a student sends, with no status field), `ResourceReview.java` (staff decision + note).
- `repository/ResourceRepository.java` (new): the SQL. Edits and deletes use `WHERE ... AND user_id = ? AND status = 'pending'`, so the check and the change are one statement and can't race an approval. Audiences are saved in the existing `resource_audiences` table.
- `service/ResourceService.java` (new): validation (all bad fields listed at once), students -> `pending`, staff/admin -> `approved` right away, 403 for someone else's resource, 409 `NOT_PENDING` once reviewed, review with `approved` / `rejected` / `pending` (on hold) + optional note.
- `controller/ResourceController.java` (new): `GET /resources` (public, approved only), `GET /resources?status=pending|rejected` (staff/admin), `POST /resources`, `PUT` / `DELETE /resources/{id}`, `PUT /resources/{id}/status` (staff/admin), `GET /users/me/resources`.
- `common/AuthInterceptor.java` + `WebConfig.java`: the rules above.
- `data.sql`: the 11 sample resources from the Resources page are now real rows, plus 2 pending student submissions for the review queue.
- Tests: `ResourceServiceTest.java` (new, 15 tests), 4 new tests in `AuthInterceptorTest.java`.

**Frontend**
- `api/resourcesApi.js`: sample data removed, now calls the real endpoints; new submitResource, updateResource, deleteResource, getMyResources, getReviewQueue, reviewResource, formatDate.
- `components/ShareResourceModal.jsx` (new): share / edit form in a pop-up.
- `components/MyResourcesPanel.jsx` (new): "My submissions" with a status badge (Pending review / Posted / Denied), the staff note, and Edit/Delete only while pending.
- `components/ResourceReviewPanel.jsx` (new): staff tabs Pending / Posted / Denied, with Approve, Deny, Put on hold and a note.
- `components/ResourceSubmissions.css` (new): styles for the three, same colors as the Resources page.
- `components/ResourceCard.jsx`: no "Open" button when a resource has no link.
- `pages/ResourcesPage.jsx`: `reloadKey` prop; the disabled share button now says "Log in to share a resource".
- `App.jsx`: opens the share pop-up, shows My submissions when logged in and the review panel for staff/admin.
- `api/resourcesApi.test.js`: the sample-data test is replaced by fetch-mocked tests for every new call.

## How it was checked
- The Java service, controller, repository and auth tests were compiled and run against small stand-ins for Spring and JUnit (no Maven packages in the AI's sandbox): 15 + 33 passing. Still to do on our machines: `./mvnw test`, and start the backend once so `schema.sql` / `data.sql` run on PostgreSQL (the new SQL was not run against a real database).
- `resourcesApi.test.js` was run with a small stand-in for Vitest: 12 passing. Still to do: `npm test`.
- The pages were opened in Chrome with a fake backend as a student (share, see status, delete while pending) and as staff (approve moves it out of Pending and into the directory).

## Changed or rejected
- "Deny" is stored as `rejected`, the value the table's CHECK rule and Interface Contract v7 already use; the website shows it as "Denied".
- "On-hold posts" = `pending`. Staff can put any resource back on hold, which also lets the student edit it again (e.g. to fix a broken link).
- Rejected letting students send `status`: the request body has no status field, so only the server sets it.
- Staff and admin can both review (same `isStaff()` rule as events and jobs), since the story says "admin" but the sprint plan says Career Services staff.
- Rejected `GET /resources/{id}`: nothing needs it yet, and it would need a "who can see a pending one" rule.
- The public list hides `userId` and `reviewNote`; only the person who shared it and staff/admin see them.
- Student names aren't shown ("Student submission"): our token only gives a user id, and names live in Shared Core.
- The Admin subsystem's moderation dashboard can read the same `GET /resources?status=pending` and call `PUT /resources/{id}/status`; not wired yet (open question for Admin).
