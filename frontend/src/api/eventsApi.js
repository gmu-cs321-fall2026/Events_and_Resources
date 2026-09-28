const STORAGE_KEY = "careerEvents";

export function getEvents() {
    const events = localStorage.getItem(STORAGE_KEY);

    if (events) {
        return JSON.parse(events);
    }

    return [];
}