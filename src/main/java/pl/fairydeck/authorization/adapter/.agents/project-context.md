<!-- BEGIN project-context-initializer:context -->
# adapter

Path `src/main/java/pl/fairydeck/authorization/adapter` | source `5debbf6` (affected scope) | refreshed 2026-09-11T09:42:24Z | coverage: own

**Slices (each rolled up here; ArchUnit keeps them independent).**
- `in/rest`: `AuthorizationController` (`POST /v1/authorizations`, `/{id}/capture`, `/{id}/reverse`),
  `CardController` (`POST /v1/cards`, `GET /v1/cards/{id}/balance`, `GET /v1/cards/{id}/transactions`),
  request records with Jakarta validation, response records, `ApiExceptionHandler` (404 / 409 / 400 problem details).
- `out/persistence`: `JdbcCardRepository`, `JdbcLedgerRepository` (advisory lock, insert-only),
  `JdbcAuthorizationRepository` (upsert on id, unique idempotency key -> `DuplicateIdempotencyKeyException`,
  expired holds, filtered listing), `Columns` (money and UTC timestamps). Schema: `src/main/resources/db/migration/V1__cards_authorizations_ledger.sql`
  (tables, indexes, append-only trigger), `V2__outbox_events.sql`.
- `out/cache`: `RedisCardCache` (`card:{uuid}`, TTL from `CardCacheProperties`, outage -> miss),
  `CardCacheConfiguration` (`RedisTemplate<String, Card>` with Jackson 3 record-only serialization).
- `out/risk`: `HttpRiskScorer` (`POST /v1/scores`; transport/null, fractional and out-of-`int` scores -> `Unavailable`
  through `intValueExact()`),
  `RiskScoringConfiguration` (`RestClient` from Boot's builder + `RiskScoringProperties.baseUrl`).
- `out/events`: `JdbcOutbox` (JSONB payload record), `OutboxRelay` (`@Scheduled` + `@Transactional`,
  `FOR UPDATE SKIP LOCKED`, at-least-once), `EventPublisher` seam, `LoggingEventPublisher`, `OutboxProperties`.

**Inbound / outbound contracts.** HTTP JSON as in README; Postgres tables above; Redis JSON values; risk engine
request `{cardId, amount, currency, merchant}` / response `{score}`; outbox payload
`{eventId, authorizationId, cardId, status, amount, currency, occurredAt}` typed `authorization.<status>`.

**Configuration.** `cache.cards.time-to-live`, `risk.scoring.base-url`, `outbox.relay-interval`,
`outbox.batch-size`, `spring.mvc.problemdetails.enabled`.

**Rolled-up directories.** `in/rest`, `out/cache`, `out/events`, `out/persistence`, `out/risk`,
`src/main/resources/db/migration`; specs in `src/test/java/.../adapter/**` (web slices, cache outage, WireMock
contract) and `src/integrationTest/java/.../adapter/out/**` (repositories, cache, outbox).

**Tests.** `./gradlew test --tests 'pl.fairydeck.authorization.adapter.*'`;
`./gradlew integrationTest --tests 'pl.fairydeck.authorization.adapter.*'` (Docker).

**Invariants.** Slices do not import each other (`ArchitectureTest`); persistence never updates or deletes
ledger rows (database trigger); adapters translate failures into port-level meaning, they do not decide.
The REST adapter's existing `IllegalArgumentException` problem-details mapping makes an unrepresentable input amount a 400.

**Git signals.** `JdbcAuthorizationRepository.java` 4 commits, `ApiExceptionHandler.java` 3, co-change with the
port and the integration spec.

**Risks.** Real risk engine contract unknown (risk 7); broker seam only (risk 6).

**Evidence.** `src/main/java/pl/fairydeck/authorization/adapter/CLAUDE.md`, `DECISIONS.md` §5, §6, §8, §9, §12, §14.
<!-- END project-context-initializer:context -->
