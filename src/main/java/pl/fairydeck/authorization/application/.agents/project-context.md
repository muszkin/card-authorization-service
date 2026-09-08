<!-- BEGIN project-context-initializer:context -->
# application

Path `src/main/java/pl/fairydeck/authorization/application` | source `b4bef16` | refreshed 2026-09-07T14:57:44Z | coverage: own

**Responsibilities.** Use cases and transaction boundaries: `AuthorizePurchase` (idempotent replay, card
lookup, risk call outside the transaction, race recovery), `AuthorizationBooking` (`@Transactional(READ_COMMITTED)`:
ledger lock, balance, policy, authorization + hold + outbox event), `AuthorizationLifecycle` (capture, reverse,
scheduled `releaseExpiredHolds`; a retried capture or reversal is replayed from the post-lock status), `CardLookup` (cache-aside), `CardQueries` (balance, filtered transactions),
`IssueCard`, `AuthorizationConfiguration` (`Clock.tickMillis(UTC)`, `AuthorizationPolicy` from
`AuthorizationProperties`), exceptions `CardNotFoundException`, `AuthorizationNotFoundException`,
`IdempotencyKeyReusedException`.

**Ports (`port/out`).** `CardRepository`, `LedgerRepository` (`lock(cardId)` before any balance-changing write),
`AuthorizationRepository` (`findByIdempotencyKey`, `findExpiredHolds`, `findByCard(TransactionFilter)`),
`CardCache`, `RiskScorer`, `Outbox`, `DuplicateIdempotencyKeyException`, `TransactionFilter`.

**Consumers / providers.** Called by `adapter/in/rest`; ports implemented by `adapter/out/*`; fakes in
`src/testFixtures/java/pl/fairydeck/authorization/application/port/out/InMemory*.java`, `StubRiskScorer.java`.

**Rolled-up.** `port/out`; unit specs `src/test/java/pl/fairydeck/authorization/application/**`; integration
specs `src/integrationTest/java/pl/fairydeck/authorization/application/ConcurrentAuthorizationsTest.java`;
fixtures `src/testFixtures/java/pl/fairydeck/authorization/application/port/out`.

**Configuration.** `authorization.max-acceptable-risk-score` (70), `authorization.hold-validity` (7d),
`authorization.expiry-sweep-interval` (PT1M).

**Tests.** `./gradlew test --tests 'pl.fairydeck.authorization.application.*'`;
`./gradlew integrationTest --tests '*ConcurrentAuthorizationsTest'` (Docker).

**Invariants.** Remote calls never inside a transaction; every balance-changing transaction takes the card
ledger lock before reading the ledger; the use case re-reads an authorization under the lock before a
transition; a capture retried on `CAPTURED`, or a reversal retried on `REVERSED` or `EXPIRED`, returns the current
authorization without new ledger entries or events (DECISIONS §17), every other impossible transition still
throws; no imports from `adapter`.

**Git signals.** `AuthorizationBooking.java` 3 commits, `AuthorizationRepository.java` 3 commits; port and JDBC
implementation co-change.

**Risks.** Expiry sweep batch transaction (risk 3); broad `IllegalArgumentException` mapping is in the adapter but
originates from domain validation.

**Evidence.** `src/main/java/pl/fairydeck/authorization/application/CLAUDE.md`, `DECISIONS.md` §4, §5, §8, §9, §13.
<!-- END project-context-initializer:context -->
