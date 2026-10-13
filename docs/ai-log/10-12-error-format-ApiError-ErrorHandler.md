# AI Log — Standard error format

- **Date:** 10/12/2026 · **Author:** Yasmin · **AI tool:** Claude (claude.ai)
- **Commit:** `fef1d5a` "Add standard error format: ApiError, ApiException, ErrorHandler"
- **Files:** `backend/.../common/ApiError.java`, `ApiException.java`, `ErrorHandler.java`

## Asked
Asked Claude to walk me through my Sprint 2 tickets (MASON-34, MASON-33, MASON-6) and then build the first piece: a shared way for every endpoint to return errors in the team format `{"error", "code", "detail"}` (SOW §6). Later asked why `ErrorHandler` is so long, and to add comments explaining every import and handler.

## Produced
- `ApiError`: a record with `error`, `code`, `detail`.
- `ApiException`: one exception class carrying an HTTP status and code, with shortcut methods (`notFound`, `conflict`, `badRequest`, `unauthorized`, `forbidden`).
- `ErrorHandler` (`@RestControllerAdvice`) with 5 handlers: our own `ApiException`, wrong type in the URL (400), unknown URL (404), wrong HTTP method (405), and a catch-all for unexpected errors (500, logs the stack trace).
- A temporary `TempErrorTestController` with two URLs that throw on purpose, used only for testing.
- When I asked why the file is long, it also offered a shorter 2-handler version.

## Changed or rejected
- **Kept all 5 handlers and rejected the shorter 2-handler version.** Without the extra three, Spring answers bad URLs, unknown paths, and wrong methods in its own JSON format instead of ours, and our Interface Contract v1 already promises `400 VALIDATION_ERROR` and `405 METHOD_NOT_ALLOWED` in our format. The Q&I team tests against the contract.
- **Kept the catch-all 500 handler as-is** because it hides Java error details from users but still logs them for us. I checked this with `curl /test/boom`: the response was only `INTERNAL_ERROR`, and the stack trace appeared in the server terminal.
- **Kept one `ApiException` with shortcuts instead of separate `NotFoundException`/`ConflictException` classes** (which the first walkthrough showed). It is less code and the status/code always travel together, so a handler can't pair the wrong status with a code.
- **Rejected/deleted `TempErrorTestController` before committing.** It only existed to trigger errors for testing; nothing should depend on it.
- **Edited:** asked for more inline comments (imports, each handler, the helper methods) so teammates can maintain it.
- **How I verified before keeping it:** ran 5 curl tests: `/test/full/5` → 409 `EVENT_FULL`, `/test/full/abc` → 400 `VALIDATION_ERROR` ("id: must be a valid long"), `/nope` → 404, `DELETE /hello` → 405, `/test/boom` → 500. All five returned our `{"error", "code", "detail"}` shape.
