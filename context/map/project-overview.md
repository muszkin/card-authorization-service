<!-- BEGIN project-context-initializer:artifact -->
# Project overview

Affected-scope refresh at `5debbf647734dadcae75960f405ba5602a9bc82b` on 2026-09-11T09:42:24Z. Historical sections retain their earlier evidence unless stated below. Navigation evidence only: current user instructions, code, tests, runtime behaviour and canonical docs outrank this file.

**Purpose (Observed, README.md):** a small Spring Boot service that authorizes card purchases. A purchase is
checked against a card's status, the balance available on its credit limit and an external risk score; the
decision is booked in one transaction together with a ledger hold and an outbox event. The balance is never
stored; it is derived from an append-only ledger.

**Actors and surfaces (Observed, `adapter/in/rest`):**

| Actor | Surface | Evidence |
| --- | --- | --- |
| Card issuer / back office | `POST /v1/cards` | `CardController.java` |
| Merchant / payment switch | `POST /v1/authorizations` (+ `/{id}/capture`, `/{id}/reverse`) | `AuthorizationController.java` |
| Cardholder-facing apps | `GET /v1/cards/{id}/balance`, `GET /v1/cards/{id}/transactions` | `CardController.java` |
| Risk engine (outbound) | `POST {risk.scoring.base-url}/v1/scores` | `HttpRiskScorer.java` |
| Event consumers (outbound) | outbox relay -> `LoggingEventPublisher` (broker seam) | `OutboxRelay.java` |

**Repository boundaries (Observed):** one Gradle module, `scope_kind=repository`, packages
`domain / application / adapter / technical` under `pl.fairydeck.authorization`, boundaries enforced by
`src/test/java/.../architecture/ArchitectureTest.java`.

**Primary flows:** see [architecture-and-flows.md](architecture-and-flows.md). Purchase authorization,
capture/reversal, scheduled expiry sweep, outbox relay, card issuing and read queries.

**Maturity (Observed):** recruitment-task deliverable finished 2026-09-07; the numeric-boundary checkpoint at
`5debbf6` records 120 tests, 20 integration tests and 12 acceptance scenarios passing before final assembled
delivery gates. Historical CI is green; feature CI is not run at that checkpoint. Local run was verified through
`compose.yaml`. No deployment target, no authentication,
no metrics (`Decided`, README "Known limitations" and DECISIONS.md §15).

**Explicit non-goals (Decided, DECISIONS.md §15):** caller authentication, OpenAPI, metrics/tracing,
Kubernetes manifests, partial captures, multi-currency cards, refund endpoints, typed identifiers, JSpecify
`@NullMarked` everywhere.

**Evidence map:** `README.md`, `DECISIONS.md`, `AI_USAGE.md`, `CLAUDE.md` (root and per layer),
`build.gradle.kts`, `src/main/resources/application.yaml`, `src/main/resources/db/migration/*.sql`,
`src/acceptanceTest/resources/.../card_authorization.feature`.
<!-- END project-context-initializer:artifact -->
