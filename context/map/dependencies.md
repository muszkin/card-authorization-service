<!-- BEGIN project-context-initializer:artifact -->
# Dependencies

Affected-scope refresh at `5debbf647734dadcae75960f405ba5602a9bc82b` on 2026-09-11T09:42:24Z. Historical sections retain their earlier evidence unless stated below. Navigation evidence only: current user instructions, code, tests, runtime behaviour and canonical docs outrank this file.

## Tree (Observed from imports; `ArchitectureTest.java` enforces the direction)

```text
adapter.in.rest ──> application ──> domain
adapter.out.persistence ──> application.port.out, domain      (Postgres via JdbcClient, Flyway)
adapter.out.cache       ──> application.port.out, domain      (Redis via RedisTemplate)
adapter.out.risk        ──> application.port.out, domain      (HTTP via RestClient)
adapter.out.events      ──> application.port.out, domain      (Postgres outbox table, log publisher)
technical.httpclient    ──> technical.logbook                 (correlation header)
Boot RestClient.Builder <── technical.httpclient customizer    (used by adapter.out.risk)
```

## Edges

| From | To | Contract or reason | Direction | Evidence | Status |
| --- | --- | --- | --- | --- | --- |
| `AuthorizationController`, `CardController` | `AuthorizePurchase`, `AuthorizationLifecycle`, `IssueCard`, `CardQueries` | use case calls | in -> application | `adapter/in/rest/*.java` | Observed |
| `AuthorizePurchase` | `CardLookup`, `RiskScorer`, `AuthorizationBooking`, `AuthorizationRepository` | orchestration | application internal / ports | `AuthorizePurchase.java` | Observed |
| `AuthorizationBooking` | `LedgerRepository`, `AuthorizationRepository`, `Outbox`, `AuthorizationPolicy`, `Clock` | one transaction | application -> ports/domain | `AuthorizationBooking.java` | Observed |
| `JdbcCardRepository` / `JdbcLedgerRepository` / `JdbcAuthorizationRepository` | `CardRepository` / `LedgerRepository` / `AuthorizationRepository` | port implementations | out -> port | `adapter/out/persistence` | Observed |
| `RedisCardCache` | `CardCache` | port implementation, TTL, outage -> miss | out -> port | `adapter/out/cache` | Observed |
| `HttpRiskScorer` | `RiskScorer` | port implementation; transport, null, fractional and out-of-`int` scores -> `Unavailable` | out -> port | `adapter/out/risk/HttpRiskScorer.java` | Observed |
| `JdbcOutbox` / `OutboxRelay` / `LoggingEventPublisher` | `Outbox`; `EventPublisher` (adapter-local seam) | outbox pattern | out -> port | `adapter/out/events` | Observed |
| `OutboundHttpConfiguration` | Boot `ClientHttpRequestFactoryBuilder` | customizer (retry, correlation) | technical -> framework | `technical/httpclient` | Observed |
| `CorrelationIdHeader` | `CorrelationId` (logbook) | MDC read | technical.httpclient -> technical.logbook | `CorrelationIdHeader.java` | Observed |
| application | PostgreSQL 17 | tables, advisory lock, trigger | runtime | `db/migration/V1`, `V2` | Observed |
| application | Redis 7 | `card:{uuid}` JSON with TTL | runtime | `RedisCardCache.java` | Observed |
| application | risk engine | `POST /v1/scores` `{cardId, amount, currency, merchant}` -> `{score}` | runtime, external | `HttpRiskScorer.java`, `local/risk-engine/mappings/scores.json` | Observed (contract with a stub; real service Unknown) |
| `OutboxRelay` | message broker | none yet; `LoggingEventPublisher` logs the payload | runtime, external | `LoggingEventPublisher.java` | Unknown (seam only) |

## Hubs, cycles, unknowns

- High fan-in: `AuthorizationRepository` (4 implementations/consumers touch it: JDBC adapter, fake, three use cases),
  `Money` (every layer). No cycles: layered rule and slice independence hold (`ArchitectureTest`, green).
- Unknown edges: the real risk engine contract beyond the stub; the production event broker; any API gateway.

## Delivery dependencies (Observed)

Gradle wrapper -> Maven Central; Testcontainers -> local Docker (images `postgres:17-alpine`, `redis:7-alpine`);
`compose.yaml` adds `wiremock/wiremock:3.13.2`; the configured CI runner is `[self-hosted, home]` and must provide Docker.
<!-- END project-context-initializer:artifact -->
