<!-- BEGIN project-context-initializer:artifact -->
# Architecture and flows

Affected-scope refresh at `5debbf647734dadcae75960f405ba5602a9bc82b` on 2026-09-11T09:42:24Z. Historical sections retain their earlier evidence unless stated below. Navigation evidence only: current user instructions, code, tests, runtime behaviour and canonical docs outrank this file.

## Layers (Observed, `ArchitectureTest.java`)

| Layer | Package | May depend on | Notes |
| --- | --- | --- | --- |
| domain | `pl.fairydeck.authorization.domain.{money,card,ledger,authorization}` | nothing framework-specific | records + `Authorization` class with transitions; rules in `AuthorizationPolicy` |
| application | `pl.fairydeck.authorization.application` (+ `port/out`) | domain, Spring | use cases, transaction boundaries, ports |
| adapter | `adapter/in/rest`, `adapter/out/{persistence,cache,risk,events}` | application, domain, technical, Spring | slices are independent of each other |
| technical | `technical/{logbook,httpclient}` | Spring, libraries | never domain or application |

Diagram: [diagrams/module-dependencies.mmd](diagrams/module-dependencies.mmd) (rendering `not-run`; conservative syntax).

## Runtime entrypoints (Observed)

- HTTP: `AuthorizationController` (`/v1/authorizations`, `/{id}/capture`, `/{id}/reverse`), `CardController`
  (`/v1/cards`, `/{id}/balance`, `/{id}/transactions`).
- Scheduled: `OutboxRelay.relayPendingEvents()` every `outbox.relay-interval` (PT1S);
  `AuthorizationLifecycle.releaseExpiredHolds()` every `authorization.expiry-sweep-interval` (PT1M).
- Servlet filter: `RequestLoggingFilter` (correlation id, one log line per request).

## Primary flow: authorize a purchase (Observed)

Diagram: [diagrams/primary-runtime-flow.mmd](diagrams/primary-runtime-flow.mmd).

1. `AuthorizationController.authorize` validates `AuthorizationRequest`, requires `Idempotency-Key`, builds a
   domain `Purchase` (`Money.of` rejects amounts finer than the currency allows and converts an unrepresentable
   minor-unit value to `IllegalArgumentException`, which the existing problem-details handler maps to HTTP 400).
2. `AuthorizePurchase.authorize`: `AuthorizationRepository.findByIdempotencyKey` -> replay if known
   (`Authorization.isFor` guards against key reuse -> `IdempotencyKeyReusedException`, HTTP 409).
3. `CardLookup.find`: `CardCache` (Redis, TTL `cache.cards.time-to-live`) then `CardRepository`; miss not cached;
   unknown card -> `CardNotFoundException` (404) before any risk call.
4. `RiskScorer.assess` -> `HttpRiskScorer` POST `/v1/scores`; `RestClientException`, a null response/score, or a
   fractional or out-of-`int` score (`BigDecimal.intValueExact()`) -> `RiskAssessment.Unavailable`. Outside the
   transaction on purpose.
5. `AuthorizationBooking.book` (`@Transactional(isolation = READ_COMMITTED)`): `LedgerRepository.lock(cardId)`
   (`pg_advisory_xact_lock`), `Balance.derive`, `AuthorizationPolicy.authorize`, save authorization, append `HOLD`
   if approved, `Outbox.record(AuthorizationEvent)`.
6. Racing identical retry: unique `idempotency_key` -> `DuplicateIdempotencyKeyException` -> loser re-reads the winner.
7. Response `201` with `AuthorizationResponse`; `Location: /v1/authorizations/{id}`.

## Other flows (Observed)

- Capture / reverse: `AuthorizationLifecycle.settle` loads, locks the card ledger, re-reads, applies
  `Authorization.capture` (HOLD_RELEASE + CAPTURE) or `reverse` (HOLD_RELEASE), saves, records event.
  Illegal transition -> `IllegalStateException` -> 409.
- Expiry sweep: `findExpiredHolds(now)` (batch 100) -> `Authorization.expire` -> HOLD_RELEASE, status EXPIRED.
- Outbox relay: `lockPending(batch)` with `FOR UPDATE SKIP LOCKED`, publish, `markPublished`; at-least-once.
- Balance: `CardQueries.balance` = `Balance.derive(card.creditLimit, ledger.entriesFor(cardId))`.
- Transactions: `CardQueries.transactions` with `TransactionFilter` (from inclusive, to exclusive, status).

## Data and state (Observed, `db/migration`)

`cards`, `authorizations` (unique `idempotency_key`, index `(card_id, created_at DESC)`), `ledger_entries`
(identity `position`, index `(card_id, position)`, trigger `ledger_entries_append_only` rejects UPDATE/DELETE),
`outbox_events` (JSONB payload, partial index on pending). Money stored as `*_minor BIGINT` + `currency CHAR(3)`.
Redis key `card:{uuid}` holds the card as JSON.

## Trust and security boundaries

No caller authentication (`Decided`, out of scope). Inbound: JSON validation + problem details. Outbound: risk
engine over HTTP with 100 ms connect / 150 ms read timeouts, one immediate retry on 429/503, never on timeout.
Secrets: none in the repository; local compose credentials are development-only (`compose.yaml`).

## Observability

ECS JSON console logs, `X-Correlation-Id` propagated in (filter) and out (`CorrelationIdHeader`), per-request
latency line. No metrics or tracing (`Decided`).

## Deployment shape

Executable Boot jar; `compose.yaml` for local infrastructure only. Production topology `Unknown` (not defined
in the repository).

## Contradictions

None open. See [risks-and-unknowns.md](risks-and-unknowns.md) item 1 for the one found and resolved during
initialization.
<!-- END project-context-initializer:artifact -->
