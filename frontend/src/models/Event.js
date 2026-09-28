class Event {
    constructor(eventTitle, host, date, time, location, tag, registerLink) {
        this.eventTitle = eventTitle;
        this.host = host;
        this.date = date;
        this.time = time;
        this.location = location;
        this.tag = tag;
        this.registerLink = registerLink;
    }
}

export default Event;