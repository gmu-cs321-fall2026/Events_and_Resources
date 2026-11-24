# AI log: publish our Docker image to GHCR (11-24)

**Asked:** Our CI builds the Docker image but never publishes it, so other teams can't run our subsystem in the integration docker-compose. Make a GitHub Actions workflow that publishes our image to GitHub Container Registry (GHCR).

**Produced:** Claude wrote `.github/workflows/publish.yml`. It runs on every push to `main` (and has a manual "Run workflow" button). It logs in to GHCR with the built-in `GITHUB_TOKEN`, builds our existing Dockerfile for both amd64 and arm64 (so it runs on Windows/Linux PCs and Apple Silicon Macs), and pushes `ghcr.io/gmu-cs321-fall2026/events_and_resources` with two tags: `latest` (newest main) and `sha-<commit>` (one exact version other teams can pin).

**Changed or rejected:** Kept it as a separate file instead of adding it to `ci.yml`, so the `lint-and-test` check the branch ruleset needs stays the same. It only runs on `main`, not on pull requests, so other teams only get code that passed review and CI.
