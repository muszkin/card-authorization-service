# Agent guide for card-authorization-service

Java 25, Spring Boot 4.1, Gradle 9. Single module, hexagonal packages, boundaries enforced by ArchUnit. Read
[README.md](README.md) for what the service does and [DECISIONS.md](DECISIONS.md) before changing anything the
decisions cover. Each layer has its own short `CLAUDE.md` under `src/main/java/pl/fairydeck/authorization/`.

<!-- BEGIN project-context-initializer:router -->
## Generated project context

@context/map/INDEX.md
<!-- END project-context-initializer:router -->

## Working rules

- Test first, and watch the test fail before writing production code. A new type may fail to compile in the
  test source set; an existing behaviour must fail on an assertion. Then make it pass with the smallest change.
- `./gradlew test` must stay runnable without Docker. Anything that needs Postgres or Redis goes into
  `src/integrationTest`; anything that drives the whole application over HTTP goes into `src/acceptanceTest`.
- Unit tests use the in-memory fakes in `src/testFixtures`; reach for Mockito only when a real collaborator
  cannot fail on demand.
- Conventional Commits in English. `test:` commits may leave the build red; the next `feat:` commit turns it
  green. Never amend, squash or force-push.
- Constructor injection only. No `@Autowired` on fields; ArchUnit fails the build otherwise.
- Money is `Money` (minor units plus `java.util.Currency`). No `double`, no hard-coded `* 100`.
- The ledger is append-only and the database enforces it. Corrections are new entries.
- Any transaction that changes a card's balance calls `LedgerRepository.lock(cardId)` before reading the ledger.
- Keep it small. If a change is not needed by a test or the brief, write it down in `DECISIONS.md` as rejected
  instead of adding it.
- No secrets in code, configuration or history. Local credentials live only in `compose.yaml`.

## Commands

```bash
./gradlew test               # fast suite, no Docker
./gradlew integrationTest    # Testcontainers
./gradlew acceptanceTest     # Cucumber over HTTP
./gradlew build              # everything
./gradlew bootRun            # local run with compose.yaml
```

## Skills

`.claude/skills/task-closeout` is the closing step of a piece of work: it triages what was learned into an ADR
entry, an instruction rule, agent memory or a work-log note, and discards the rest.
