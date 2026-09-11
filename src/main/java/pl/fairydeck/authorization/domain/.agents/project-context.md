<!-- BEGIN project-context-initializer:context -->
# domain

Path `src/main/java/pl/fairydeck/authorization/domain` | source `5debbf6` (affected scope) | refreshed 2026-09-11T09:42:24Z | coverage: own

**Responsibilities.** Business rules with no framework dependency: `money/Money` (minor units + `Currency`
exponent, rejects excess precision and wraps an unrepresentable minor-unit conversion as `IllegalArgumentException`,
`CurrencyMismatchException`), `card/Card` (+ `CardStatus`, `Card.issue`), `ledger/LedgerEntry`
(append-only fact, positive amount), `ledger/LedgerEntryType` (HOLD, HOLD_RELEASE, CAPTURE, REFUND),
`ledger/Balance` (`derive(creditLimit, entries)`: pending, settled, available), `authorization/Authorization`
(states APPROVED, DECLINED, CAPTURED, REVERSED, EXPIRED; `hold()`, `capture(now)`, `reverse(now)`, `expire(now)`,
`isFor(purchase)`), `authorization/AuthorizationPolicy` (rules in order: card status, available balance, risk;
unavailable risk declines), `authorization/RiskAssessment` (sealed: `Scored`, `Unavailable`), `Purchase`,
`DeclineReason`, `AuthorizationEvent`.

**Consumers.** `application` (use cases, ports) and `adapter` (mapping to rows, JSON, HTTP).

**Rolled-up.** `money`, `card`, `ledger`, `authorization` packages; unit specs in
`src/test/java/pl/fairydeck/authorization/domain/**`.

**Tests.** `./gradlew test --tests 'pl.fairydeck.authorization.domain.*'` (JUnit 6 + AssertJ, no Spring).

**Invariants.** No Spring/Jakarta imports (ArchUnit `domainIsFreeOfFrameworks`); amounts never `double`; parsed input
must fit signed `long` minor units or fail as `IllegalArgumentException` rather than leaking `ArithmeticException`;
ledger entries carry direction in their type, not in the sign; a correction is a new entry; `Authorization`
transitions throw `IllegalStateException` for impossible moves and return the ledger entries that record them.

**Git signals.** `Authorization.java` changed in 4 commits (decision -> idempotency -> transitions).

**Risks.** None specific; see repository risks for the size discussion.

**Evidence.** `src/main/java/pl/fairydeck/authorization/domain/CLAUDE.md`, `DECISIONS.md` §2, §3, §7, §13, the classes listed above.
<!-- END project-context-initializer:context -->
