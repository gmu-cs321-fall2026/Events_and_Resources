\## Asked

Asked ChatGPT to write a helper function request for eventsApi,js that calls a mock from mockEventsApi. This function will send a request and returns the JSON body or throw an error message: if the response is not in status 200-299, error will have this format "error": "...", "code": "...", "detail": "...", If the backend is down, the proxy answers 500/502 with no JSON body.



\## Produced

A helper function request(path, options = {}) with direct parsing to JSON and simple error handlings (without explicit network/backend unreachable handling)



\## Changed or rejected

Kept the idea of this function but added more details in handling errors, used fetch to send requests, preserved HTTP status and error code, read the body as text first before try to parse it into JSON to limit an empty body or an HTML error page cases that could crash json.

