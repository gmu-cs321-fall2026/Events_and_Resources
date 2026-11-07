# AI Log: S4-01 Connect to Shared Core's real login

**AI tool:** Claude by Yasmin
**Date:** 11-06

## Asked
Asked for the last part of "Prepare for Shared Core integration": call Shared Core's real `GET /auth/validate` instead of only the fake test tokens, without breaking tests/CI (which can't run Shared Core).

## Produced
- `SharedCoreTokenValidator`: calls `{SHARED_CORE_URL}/auth/validate` with the user's token (2-second timeout). 200 -> `CurrentUser` (roles lowercased); 401 -> invalid token (our 401); 403 -> 403 `ACCOUNT_SUSPENDED`; other errors or no answer -> 503 `SERVICE_DOWN`.
- `AUTH_MODE` setting picks the validator: `fake` (default, tests/CI) or `shared-core`. Only one loads at a time, so `AuthInterceptor` didn't change.
- `AUTH_MODE` / `SHARED_CORE_URL` added to the Dockerfile and docker-compose.yml.
- `SharedCoreTokenValidatorTest`: 6 tests against a fake Shared Core (`MockRestServiceServer`).

## Changed or rejected
- Test file was first created under main/; moved it to test/.../integration/ so JUnit and MockRestServiceServer are available.