\## Asked

Asked ChatGPT to write helper functions for EventDetailModal.jsx to format date and time: turn "2026-10-20" and "15:00" into "Tuesday, October 20, 2026" and "3:00 PM", and useEffect() for 2 effects of the pop-up. a) load the event when pop-up opens that get event by id and catch error 404 if id does not exist, b) close the pop-up when user presses close that listen on the whole window and the clean up removes the listener when the pop-up closes



\## Produced

Two functions to format date and time using toLocaleDateString and toLocaleTimeString without handling invalid date error. It does not use ignore to clean up but removes the listener



\## Changed or rejected

Added T:00:00 to avoid UTC date shifting. Handled invalid dates to fall back to original value. Used ignore to clean up

