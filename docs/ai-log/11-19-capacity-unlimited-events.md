# AI Log: MASON-19 fix, staff/admin can change capacity on unlimited events

## Asked
"I can only update capacity in in-person format as admin, not in virtual or all formats."

## Found
Format wasn't the cause. The pop-up showed the capacity editor (WaitlistPanel) only when `event.capacity > 0`. In `data.sql` most virtual events (Deloitte, Federal Careers, Grad School Panel) and two in-person ones (Fall Career Fair, Spring Internship Fair) have `capacity 0` = unlimited, so they had no editor. It only looked tied to format.
A second problem: the capacity box had `min={registeredCount}`, so the browser refused 0 (unlimited) once anyone had registered.
The backend already allows 0 -> N and N -> 0 (`WaitlistService.changeCapacity`), so no Java changed.

## Produced
- `components/EventDetailModal.jsx`: staff/admin see WaitlistPanel on every event, not only limited ones. "Spots" now also shows for unlimited events: "Unlimited (N registered)".
- `components/WaitlistPanel.jsx`: for an unlimited event the heading says "Capacity · unlimited" with "no seat limit, so there's no waitlist. Set a number to limit seats." The box allows 0 (`min={0}`); the backend still rejects a limit below the registered count and the panel shows its message. Hint says "0 = unlimited".

## How it was checked
Bundled with esbuild and run in headless Chrome as staff against a stub backend with the team's error format, on a Virtual event with capacity 0 and 3 registered:
- panel shows "Capacity · unlimited"; Spots shows "Unlimited (3 registered)"
- set 2 -> backend error "Capacity can't be lower than the 3 students already registered" is shown
- set 25 -> switches to the waitlist view, Spots "3 / 25 registered"
- set 0 -> back to unlimited
Still to do on our machines: `npm test`, `npm run lint`, and the same clicks against the real backend as admin and staff.

## Changed or rejected
- Rejected hiding the editor by format: capacity has nothing to do with format, and virtual events can have limits (e.g. Zoom room size).
- Kept the waitlist list hidden for unlimited events, since nobody can be waitlisted when there's no limit.
