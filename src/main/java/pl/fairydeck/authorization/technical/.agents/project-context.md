<!-- BEGIN project-context-initializer:context -->
# technical

Path `src/main/java/pl/fairydeck/authorization/technical` | source `b4bef16` | refreshed 2026-09-07T14:57:44Z | coverage: own

**Responsibilities.** `logbook/CorrelationId` (header `X-Correlation-Id`, MDC key `correlationId`),
`logbook/RequestLoggingFilter` (establishes/echoes the id, logs method, path, status, latency per request),
`httpclient/OutboundHttp` (one immediate retry on 429/503 via `DefaultHttpRequestRetryStrategy(1, 0 ms)`, never
after a timeout; correlation header), `httpclient/CorrelationIdHeader`, `httpclient/OutboundHttpConfiguration`
(`ClientHttpRequestFactoryBuilderCustomizer` for Boot's HttpComponents factory).

**Consumers.** Spring wiring (filter and customizer are beans); `adapter/out/risk` benefits through Boot's
`RestClient.Builder` without importing this package.

**Configuration.** `spring.http.clients.imperative.factory=http-components`, `spring.http.clients.connect-timeout`
(100ms), `spring.http.clients.read-timeout` (150ms), `logging.structured.format.console=ecs`.

**Rolled-up.** `httpclient`, `logbook`; specs `src/test/java/pl/fairydeck/authorization/technical/**`
(mock servlet objects, `OutputCaptureExtension`, WireMock).

**Tests.** `./gradlew test --tests 'pl.fairydeck.authorization.technical.*'`.

**Invariants.** Never depends on `domain` or `application` (ArchUnit layer rule); MDC is cleared even when the
request fails; timeouts are never retried (budget 200-300 ms per decision).

**Git signals.** Two commits (`chore:` 10 and 11); stable since.

**Risks.** None specific.

**Evidence.** `src/main/java/pl/fairydeck/authorization/technical/CLAUDE.md`, `DECISIONS.md` §11, §12.
<!-- END project-context-initializer:context -->
