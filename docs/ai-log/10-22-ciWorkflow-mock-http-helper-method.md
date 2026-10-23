\\## Asked



Asked ChatGPT to write helper method for testing 401/403 errors that create a fake HTTP request from Spring's test library. Parameter list: String method, String path, String token. Set the method, path and authorization header without any network. If token is null, then no Authorization header







\\## Produced



A helper method request() that set method, path and token, and it omits the authorization when token is null. A helper method run() that calls preHandle exactly the way Spring would before controller runs



\\## Changed or rejected



Accepted. Tested with Junit for request helper() directly without involving authentication logic. Tested run() that created a response and threw it away (the designed interceptor never writes a status code to the response at all).

