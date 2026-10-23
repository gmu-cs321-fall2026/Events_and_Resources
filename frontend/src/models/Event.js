class Event {
    constructor(eventTitle, host, date, time, location, audience, format, category, registerLink, capacity = 0, registeredCount = 0) {
        this.eventTitle = eventTitle; //What is the event called?
        this.host = host; //Who hosts the event? (Career Services, independent organization, GMU Alumni Association,...)
        this.date = date; //What is the event date?
        this.time = time; //What time does it happen?
        this.location = location; //Where is the event?
        this.audience = audience; //Who can join the event?
	this.format = format; //Is it virtual or in-person?
	this.category = category; //What is the type of the event? (Workshop, Networking, Resume Clinic, Career Fair...)
        this.registerLink = registerLink; //Where to register online for the event?

	this.capacity = capacity; //Max seats - 0 is unlimited
	this.registeredCount = registeredCount; //How many seats that has taken?
    }
}

export default Event;