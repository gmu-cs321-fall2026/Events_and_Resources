\\## Asked



Asked ChatGPT to write tests that replace fetch with a fake and check three things:
- cancelRegistration sends a DELETE with the login token, and doesn’t crash on the server’s empty “204” answer
- cancelling when you weren't registered becomes an error with code NOT_REGISTERED
- getMyRegistration status (feeding eventId 3, registered = true, registeredAt = some random time) sends a GET to the same URL and returns the answer







\\## Produced



3 tests: the first test used simplified fake response object and checked the function did not throw an error. The second test only checked status. The third test explicitly set GET and check part of the URL only.



\\## Changed or rejected



Edited. Used a real Response object with an actual 204 status for the first test, and checked that functions returned null. Checked both status and code,and used fakeResponse() to simulate the server response for the second test. Checked the request used GET for the third test