# Decisions

Each entry records what was decided, why, and what was rejected. The order roughly follows the commit history.

## 1. One Gradle module, boundaries enforced by ArchUnit

**Decided:** a single module with `domain`, `application`, `adapter` and `technical` packages, and an
architecture test that fails the build when a dependency points the wrong way.

**Why:** the brief says *small*. Multi-module builds declare separation; ArchUnit proves it, and the proof
costs one test class instead of four build files.

**Rejected:** Gradle multi-module (heavier, same guarantee); JPMS modules (fights Spring Boot's fat jar).

## 2. The balance is derived from an append-only ledger

**Decided:** `Balance.derive(creditLimit, entries)` recomputes pending, settled and available amounts from
`HOLD`, `HOLD_RELEASE`, `CAPTURE` and `REFUND` entries every time. Postgres has a trigger that rejects
`UPDATE` and `DELETE` on `ledger_entries`.

**Why:** money movements are facts; a correction is a new fact, not an edit. A stored balance column is a cache
of those facts that can drift, and drift in a ledger is an audit finding. Enforcing append-only in the schema
means no code path, including a manual fix at 3 a.m., can rewrite history.

**Rejected:** a `balance` column updated in place (lost updates, no audit trail); computing the balance in SQL
with `SUM` (correct too, but the rule would live in the adapter rather than the domain).

**Known cost:** every authorization reads the card's whole ledger. Fine at demo scale; at production scale the
answer is a periodic snapshot entry plus the delta since, which the append-only model supports without changing
the API.

## 3. Fail closed when the risk engine is unavailable

**Decided:** a timeout, a 5xx or a malformed answer from the risk engine becomes `RiskAssessment.Unavailable`,
and the policy declines with `RISK_UNAVAILABLE`.

**Why:** in regulated credit a wrongly approved purchase costs money, a chargeback and possibly a regulatory
conversation; a wrongly declined one costs a retry. The asymmetry favours declining. The decision is a rule in
the domain, not a `catch` block in the adapter, so it is visible, tested and easy to revisit.

**Rejected:** fail open (approve on timeout), acceptable only for very low amounts and only with a business
owner's signature; a per-amount threshold was left out for size but is the obvious next rule.

## 4. Concurrency guard: per-card advisory lock under READ COMMITTED

**Decided:** every transaction that changes a card's ledger first executes
`SELECT pg_advisory_xact_lock(hashtext(cardId))`, then reads the ledger, decides and writes. The transaction
runs at READ COMMITTED, stated explicitly on the method.

**Why:** the read-decide-write sequence is the textbook lost update. Once the lock is granted, READ COMMITTED
takes a fresh snapshot per statement, so the ledger read sees every hold committed by the transactions that
held the lock earlier. The concurrency test approved 11 of 10 affordable purchases without the lock and exactly
10 with it.

**Why not the other isolation levels:** REPEATABLE READ takes its snapshot at the first statement, which is the
lock wait itself, so the ledger read after the wait would be stale and the card would overdraw quietly.
SERIALIZABLE detects the conflict but resolves it by aborting one transaction with `40001`, which under
contention turns into a retry storm on the hot card.

**Why an advisory lock and not `SELECT ... FOR UPDATE` on the card row:** the card is served from the cache and
is never updated by an authorization; there is no row to lock and no reason to touch the `cards` table on the
hot path. The advisory lock is transaction-scoped, released at commit, and collisions of the 32-bit hash merely
over-serialize two unrelated cards.

**Rejected:** an atomic conditional `UPDATE cards SET available = available - ? WHERE available >= ?`, the
classic answer when a balance column exists; there is no balance column here on purpose (see 2).

## 5. Cache-aside for cards on Redis, written out explicitly

**Decided:** `CardLookup` in the application layer reads the cache, falls back to the store and remembers the
answer. Misses are not cached. The Redis adapter treats an outage as a permanent miss.

**Why:** the brief asks for exactly this pattern and it deserves to be readable, not hidden behind
`@Cacheable`. Keeping the pattern in the application layer also keeps the Redis and JDBC adapters independent
of each other, which ArchUnit checks.

**Why Redis and not an in-process cache:** several instances of this service must agree on a card's status and
limit; an in-process cache gives each instance its own opinion. The TTL bounds staleness; in this API cards are
immutable after issuing, so no invalidation is needed yet.

**Rejected:** Spring's cache abstraction (one annotation, but the fallback and the outage behaviour become
implicit); caching misses (an unknown id could shadow a card issued a moment later).

## 6. JdbcClient instead of JPA

**Decided:** repositories are thin `JdbcClient` adapters mapping rows straight onto the domain records and the
`Authorization` class. Flyway owns the schema.

**Why:** the domain model is made of immutable records and an append-only table. JPA's value is dirty checking
and lazy loading; neither applies, while its cost, a second entity model kept in sync with the domain, would
roughly double the persistence code. Explicit SQL also makes the locking and isolation decisions (see 4)
visible in the code instead of in a configuration annotation.

**Rejected:** Spring Data JPA (rejected for the reasons above, not on principle; with a mutable, richly
associated model the trade-off flips).

## 7. Money as minor units with the currency's own exponent

**Decided:** `Money(long minorUnits, Currency currency)`. Parsing goes through `new BigDecimal(String)` and the
exponent comes from `Currency.getDefaultFractionDigits()`, so `1.234 BHD` and `1200 JPY` are exact and
`12.345 GBP` is rejected rather than rounded.

**Rejected:** `double` (not exact); `BigDecimal` columns (exact but the scale rule is then a convention);
a hard-coded `* 100` (wrong for a third of the world's currencies).

## 8. Idempotency: the authorization row is the response snapshot

**Decided:** `Idempotency-Key` is mandatory, globally unique in the database, and looked up before any card
lookup or risk call. Two identical requests racing past that lookup are settled by the unique constraint:
the loser catches the port-level `DuplicateIdempotencyKeyException` and returns the winner's decision. Reusing
a key for a different purchase is a `409`.

**Why:** in payments a retry is certain, not hypothetical. Storing a separate response blob was rejected because
the authorization itself is the response; the trade-off is that a retry after a capture returns the current
state (`CAPTURED`) rather than the original `APPROVED`, which is arguably more useful and always consistent.

## 9. Transactional outbox with a polling relay

**Decided:** the decision, its hold and its event are committed together; a scheduled relay locks pending
events with `FOR UPDATE SKIP LOCKED`, publishes them and marks them published. The publisher logs; a broker
adapter would replace that one class.

**Why:** publishing from inside the request either couples the response time to the broker or loses events
on a rollback. The outbox gives at-least-once delivery with events carrying their own id for deduplication.
`SKIP LOCKED` lets several instances relay concurrently without coordination.

**Rejected:** Spring application events after commit (lost on a crash between commit and publish); CDC with
Debezium (the right answer at scale, far too much machinery here).

## 10. The test pyramid is visible in the build

**Decided:** three Gradle test suites. `test` holds unit, architecture, contract (WireMock) and web slice
tests and needs no infrastructure. `integrationTest` runs repository, cache, outbox and concurrency specs on
Testcontainers. `acceptanceTest` runs Cucumber scenarios against the whole application. Unit tests use
in-memory fakes of the ports; mocks appear only where a real collaborator cannot fail on demand (Redis outage,
web slice).

**Why:** the fastest suite is the one that gets run on every save, so it must be complete without Docker. Fakes
keep unit tests about behaviour rather than about interaction scripts.

## 11. Correlation ids and structured logs, nothing more

**Decided:** one filter establishes the correlation id, echoes it back and logs one line per request with status
and latency, which is the number the 200-300 ms authorization budget is measured against. Console logs are ECS
JSON. The outbound HTTP client forwards the id.

**Rejected:** Micrometer tracing and metrics (production-grade observability is explicitly out of scope);
Zalando Logbook (request/response body logging is a liability in a payments service).

## 12. Outbound HTTP: customize Boot's client, do not replace it

**Decided:** a `ClientHttpRequestFactoryBuilderCustomizer` adds one immediate retry for 429/503 and the
correlation header to Boot's own Apache HttpClient 5 factory. Timeouts live in `spring.http.clients.*`.
Timeouts are never retried.

**Why:** replacing the `RestClient.Builder` would silently drop Boot's message converters and observability.
A retry after a timeout would double the worst case and blow the budget; a retry after an immediate 503 is
nearly free.

## 13. Expired holds are released by a scheduled sweep

**Decided:** `AuthorizationLifecycle.releaseExpiredHolds()` runs on a schedule, finds approved authorizations
past `expiresAt` and expires them, releasing the hold and emitting an event. Captures after expiry are refused.

**Why:** this is the compensating step of the hold saga; without it money nobody claimed stays blocked.

**Rejected:** lazy expiry on next access (the balance would stay wrong until somebody looked).

## 14. Errors as RFC 9457 problem details, mapped from plain exceptions

**Decided:** `IllegalArgumentException` from the domain is a `400`, `IllegalStateException` (an impossible
transition) is a `409`, not-found exceptions are `404`. Framework failures use Spring's problem details support.

**Rejected:** a custom exception hierarchy per layer (more types, same information at this size).

## 15. Left out on purpose

- **JSpecify `@NullMarked` on every package:** without a checker in the build it is decoration; `@Nullable` is
  used where a value really can be absent.
- **Authentication and authorization of callers:** out of scope for the brief; the service would sit behind
  a gateway.
- **OpenAPI, metrics, tracing, Kubernetes manifests:** explicitly not asked for.
- **Partial captures, multi-currency cards, refunds via the API:** the ledger supports them (`REFUND` exists),
  the endpoints do not; each is a small, well-understood addition.
- **Typed identifiers (`CardId`, `AuthorizationId`):** valuable in a larger codebase, noise at this size.

## 16. The history is part of the deliverable

`test:` commits are meant to leave the build red; the following `feat:` commit makes it green. No commit was
amended or squashed. Where a new type is introduced the red build is a compile failure of the test source set,
which is what test-first looks like in a statically typed language.
