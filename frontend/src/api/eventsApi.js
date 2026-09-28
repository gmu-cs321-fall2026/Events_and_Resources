const STORAGE_KEY = "careerEvents";

export function getEvents() {
    const events = localStorage.getItem(STORAGE_KEY);

    if (events) {
        return JSON.parse(events);
    }

    return [];
}

export function createEvent(event) {
    const events = getEvents();

    events.push(event);

    localStorage.setItem(STORAGE_KEY, JSON.stringify(events));

    return event;
}