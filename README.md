# Card authorization service

[![build](https://github.com/muszkin/card-authorization-service/actions/workflows/build.yml/badge.svg)](https://github.com/muszkin/card-authorization-service/actions/workflows/build.yml)

A small Spring Boot service that authorizes card purchases. It takes a purchase against a card, reads the card
cache-aside from Redis with Postgres behind it, asks an external risk engine within a hard timeout and books the
decision in one transaction. The balance is never stored: it is derived from an append-only ledger of holds,
releases, captures and refunds.

Java 25, Spring Boot 4.1, Gradle 9, Postgres 17, Redis 7. About 1447 lines of production Java, of which
831 are statements rather than imports and braces.

## Run it

You need Docker and a JDK 25 (Gradle finds it through toolchains).

```bash
./gradlew build      # unit, integration (Testcontainers) and acceptance (Cucumber) suites
./gradlew bootRun    # starts Postgres, Redis and a stubbed risk engine from compose.yaml, then the app on :8080
```

Then, in another terminal:

```bash
CARD=$(curl -s -X POST localhost:8080/v1/cards -H 'Content-Type: application/json' \
  -d '{"cardholderId":"7c9e6679-7425-40de-944b-e07fc1f90ae7","creditLimit":500.00,"currency":"GBP"}' | jq -r .id)

curl -s -X POST localhost:8080/v1/authorizations -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d "{\"cardId\":\"$CARD\",\"amount\":42.50,\"currency\":\"GBP\",\"merchant\":\"Coffee Corner\"}"

curl -s localhost:8080/v1/cards/$CARD/balance
```

Repeat the second call with the same `Idempotency-Key` and you get the same authorization back, not a second hold.

## API

| Method | Path | What it does |
|---|---|---|
| `POST` | `/v1/cards` | Issues an active card with a credit limit in one currency |
| `POST` | `/v1/authorizations` | Decides a purchase; `Idempotency-Key` header is mandatory; `201` for both approvals and declines |
| `POST` | `/v1/authorizations/{id}/capture` | Settles an approved authorization: releases the hold, books the charge |
| `POST` | `/v1/authorizations/{id}/reverse` | Cancels an approved authorization and releases the hold |
| `GET` | `/v1/cards/{id}/balance` | Credit limit, pending, settled and available amounts, derived from the ledger |
| `GET` | `/v1/cards/{id}/transactions` | The card's authorizations, newest first; filters `from`, `to` (half-open, ISO instants) and `status` |

Decisions apply the rules in this order and stop at the first that fails: card status (`CARD_NOT_ACTIVE`),
available balance (`INSUFFICIENT_FUNDS`), risk score above the threshold (`HIGH_RISK`), risk engine
unavailable or too slow (`RISK_UNAVAILABLE`, fail-closed). Errors are RFC 9457 problem details: `400` for
invalid input, `404` for unknown cards and authorizations, `409` for a reused idempotency key or an impossible
transition.

## How it is put together

```
pl.fairydeck.authorization
  domain/        money, card, ledger, authorization: the rules, no framework
  application/   use cases and the ports they need; transactions live here
  adapter/in/    REST controllers, request and response records, problem details
  adapter/out/   persistence (JDBC + Flyway), cache (Redis), risk (HTTP), events (outbox + relay)
  technical/     correlation ids and request logging, the outbound HTTP client shape
```

The boundaries are enforced by an ArchUnit test, not by build modules. An authorization goes through
`AuthorizePurchase` (replay a known idempotency key, find the card cache-aside, ask the risk engine) and then
`AuthorizationBooking`, whose transaction takes a per-card advisory lock, derives the balance from the ledger,
applies `AuthorizationPolicy` and writes the authorization, its hold and its outbox event together. The relay
publishes events afterwards. Capture, reversal and expiry are transitions of `Authorization` itself, each
returning the ledger entries that record it.

Where the interesting answers live:

- **Concurrency and isolation:** `AuthorizationBooking` and `JdbcLedgerRepository.lock`, with the
  `ConcurrentAuthorizationsTest` that overdraws the card without the lock.
- **Idempotency:** `AuthorizePurchase`, the unique key in `V1__cards_authorizations_ledger.sql` and the racing
  retry in `AuthorizePurchaseTest`.
- **Fail-closed risk:** `AuthorizationPolicy` and `HttpRiskScorerTest`.
- **Append-only ledger:** the trigger in the first migration and `LedgerRepositoryTest`.
- **Transactional outbox:** `AuthorizationBooking`, `OutboxRelay` and `OutboxTest`.

## Tests

| Suite | Command | Needs | What it holds |
|---|---|---|---|
| Unit, architecture, contract, web slice | `./gradlew test` | nothing | domain rules, use cases on in-memory ports, ArchUnit, WireMock contract for the risk client, controllers |
| Integration | `./gradlew integrationTest` | Docker | repositories, cache, outbox and the concurrency guard on real Postgres and Redis |
| Acceptance | `./gradlew acceptanceTest` | Docker | Cucumber scenarios over HTTP against the whole application |

`./gradlew build` runs all three. Test-first commits are part of the history: `test:` commits leave the build
red on purpose and the following `feat:` commit turns it green.

## Trade-offs

Every non-obvious choice, with the alternative that lost, is in [DECISIONS.md](DECISIONS.md). The short version:
one module with ArchUnit instead of many modules, a derived balance instead of a balance column, an advisory
lock under READ COMMITTED instead of SERIALIZABLE, JdbcClient instead of JPA, fail-closed when the risk engine
is silent.

## Known limitations, and what I would do next

- Every authorization reads the card's whole ledger. The fix is periodic snapshot entries plus the delta since,
  which the append-only model supports without an API change.
- A retry of `POST /v1/authorizations` after a capture returns the authorization as it is now (`CAPTURED`),
  not the original approval; the row is the response.
- The expiry sweep processes a batch in one transaction, so one authorization that cannot be expired fails the
  whole batch until the next run.
- There is no caller authentication, no OpenAPI document and no metrics; the brief asked for a small service,
  and each of these is a known addition rather than a design question.
- Partial captures and refunds are supported by the ledger model but not exposed as endpoints.
