\\## Asked
Asked ChatGPT to create a frontend test set up for eventsApt.js: the one file where the frontend talks to the backend with fake fetch: a fake function that records how it was called and swaps it in for the browser’s real `fetch`. Each test then decides what the “server” answers with `mockResolvedValue(...)`, so no backend needs to run.

\\## Produced
A helper function that builds builds the fake answer fetch() gives back. The response is built into Node and the browser (what a real fetch() returns).


\\## Changed or rejected
Added additional beforeEach and AfterEach to set up a fresh fake before every test and restore the real fetch after, so tests don’t affect each other.



