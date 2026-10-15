\## Asked

Asked ChatGPT to write a function createEvent for saved events with their event IDs for the POST /events workflow that returns a request that tells Spring Boot this is JSON and object to JSON text



\## Produced

A createEvent(event) with one line return the request helper function



\## Changed or rejected

Kept the function that sends a POST /events request with the event object converted to JSON text, and sets the Content-Type header so Spring Boot knows the request body contains JSON

