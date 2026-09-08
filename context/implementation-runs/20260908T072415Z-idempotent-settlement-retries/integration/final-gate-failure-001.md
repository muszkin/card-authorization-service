# Final gate failure 001 — 2026-09-08T07:43:30Z

- Gate: real-surface E2E inside `./gradlew build --rerun-tasks` on assembled SHA `3ddbb487e14a749b538a3536207c492f96ac9f65`.
- Failing scenario: "A purchase within the available balance is approved" (feature line 9): expected APPROVED, got DECLINED.
- Evidence: `final-acceptance-failure.xml`; application log in the same file: "Risk scoring unavailable for card …: I/O error on POST request" for the first authorization, request log "POST /v1/authorizations responded 201 in 1005 ms" (subsequent requests 78, 55, 30, 5 ms).
- Root cause (falsifiable hypothesis, confirmed by the latency series): the very first outbound risk call runs on a cold JVM (Tomcat, Jackson, HttpClient pool, Hikari, Lettuce, WireMock) and exceeds the 150 ms read timeout; fail-closed then declines. The scenario is the first one executed, so it pays the warm-up cost.
- Classification: `flaky-or-environmental`, pre-existing in the baseline harness (the slice does not touch the authorization path; the same scenario passed at `ddd71fa` 30 minutes earlier).
- Route: repair packet R1 (acceptance harness warm-up) to a repair worker from current feature HEAD; product code and timeouts stay unchanged. Invalidated: combined gates on `3ddbb487e14a749b538a3536207c492f96ac9f65`.
