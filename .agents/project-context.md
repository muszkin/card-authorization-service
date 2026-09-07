<!-- BEGIN project-context-initializer:context -->
# Repository root context

Path `.` | scope: whole repository | source `b4bef16` | refreshed 2026-09-07T14:57:44Z | coverage: own (root)

**Purpose.** Card authorization service; see `context/map/INDEX.md` for the router and `README.md` for the
product description. This context covers everything not owned by a layer context: build, configuration, CI,
local infrastructure, test suites and agent tooling.

**Important files.** `build.gradle.kts` (Boot 4.1.1, Java 25 toolchain, suites `test` / `integrationTest` /
`acceptanceTest`, `java-test-fixtures`), `settings.gradle.kts`, `compose.yaml` + `local/risk-engine/mappings/scores.json`
(Postgres, Redis, WireMock for `bootRun`), `src/main/resources/application.yaml` (keys: `spring.http.clients.*`
timeouts, `authorization.*`, `cache.cards.time-to-live`, `outbox.*`, `risk.scoring.base-url`,
`logging.structured.format.console`), `src/main/java/pl/fairydeck/authorization/CardAuthorizationApplication.java`
(`@ConfigurationPropertiesScan`, `@EnableScheduling`), `.github/workflows/build.yml`, `.claude/settings.json`,
`.claude/skills/task-closeout/**`, `src/testFixtures/java/pl/fairydeck/authorization/{TestcontainersConfiguration,IntegrationTest}.java`.

**Rolled-up directories.** `.claude/**`, `.github/workflows`, `gradle/wrapper`, `local/risk-engine/mappings`,
`src/main/resources`, the application package root, `src/test/java/.../architecture`,
`src/integrationTest/java/pl/fairydeck/authorization` (context test), `src/testFixtures/java/pl/fairydeck/authorization`.

**Commands (verified at `b4bef16`).** `./gradlew test` (102), `./gradlew integrationTest` (20, Docker),
`./gradlew acceptanceTest` (9, Docker), `./gradlew build`, `./gradlew bootRun` (compose; port 8080).

**Invariants.** Test first with an observed red build; `test:` commits may be red, the next `feat:` is green;
no amend/squash/force-push on the delivery history (see risk 1 in `context/map/risks-and-unknowns.md`);
constructor injection only; `Money` never `double`; nothing in `domain` imports Spring or Jakarta; no secrets
anywhere; keep production code small.

**Git signals.** `application.yaml` (9 commits) and `build.gradle.kts` (8) change with almost every feature.
Single contributor. No PRs.

**Risks / unknowns.** No deployment definition; discoverability routers not smoke-tested in fresh sessions;
the §16 contradiction found during initialization is resolved.

**Child contexts.** `src/main/java/pl/fairydeck/authorization/domain/.agents/project-context.md`, `src/main/java/pl/fairydeck/authorization/application/.agents/project-context.md`,
`src/main/java/pl/fairydeck/authorization/adapter/.agents/project-context.md`, `src/main/java/pl/fairydeck/authorization/technical/.agents/project-context.md`,
`src/acceptanceTest/.agents/project-context.md`.

**Evidence.** `README.md`, `CLAUDE.md`, `DECISIONS.md`, `AI_USAGE.md`, `build.gradle.kts`, `compose.yaml`,
`.github/workflows/build.yml`, `src/main/resources/application.yaml`.
<!-- END project-context-initializer:context -->
