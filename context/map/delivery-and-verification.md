<!-- BEGIN project-context-initializer:artifact -->
# Delivery and verification

Affected-scope refresh at `5debbf647734dadcae75960f405ba5602a9bc82b` on 2026-09-11T09:42:24Z. Earlier rows are historical unless their revision says otherwise. Navigation evidence only: current user instructions, code, tests, runtime behaviour and canonical docs outrank this file.

Labels: `verified` = executed by the initializer at `b4bef16` unless a revision is given; `documented` = stated
in README/CLAUDE.md only; `derived` = inferred from configuration.

| Purpose | Command | Environment | Label | Evidence |
| --- | --- | --- | --- | --- |
| Setup | JDK 25 on the toolchain path (or `JAVA_HOME`), Docker daemon | local, CI | verified | `./gradlew javaToolchains` -> Corretto 25; `docker info` |
| Unit / architecture / contract / web slice | `./gradlew test` | no infrastructure | verified at `5debbf6`: 120 tests, 0 failures | frozen run checkpoint `RUN.md` |
| Integration | `./gradlew integrationTest` | Docker (Testcontainers Postgres 17, Redis 7) | verified at `5debbf6`: 20 tests, 0 failures | frozen run checkpoint `RUN.md` |
| Acceptance (real-surface E2E over HTTP) | `./gradlew acceptanceTest` | Docker + in-process WireMock | verified post-review at `5debbf6`: 12 scenarios, 0 failures | frozen run checkpoint `RUN.md` |
| Full build | `./gradlew build` (`check` depends on all suites) | as above | verified (fresh clone at `d34d97e`, same sources) and `--rerun-tasks` at `b4bef16` | this run |
| Local run | `./gradlew bootRun` | Docker Compose starts Postgres, Redis, WireMock | verified 2026-09-07 at `ac32950` tree (`--server.port=8090`): card issued, purchase approved, retry replayed, balance derived, capture, filtered transactions, 400 without key | smoke session, not a committed script |
| Local infrastructure only | `docker compose up -d` | Docker | derived from `compose.yaml` | `compose.yaml` |
| CI | GitHub Actions `build` on push to `main` and PRs | `[self-hosted, home]`, Corretto 25 | historical main runs succeeded; feature CI not run at the frozen checkpoint | `.github/workflows/build.yml`, frozen run checkpoint |
| Lint / format / typecheck | none configured | - | not-applicable (no tool declared) | `build.gradle.kts` |
| Release / deploy / rollback / smoke in an environment | none defined | - | Unknown (no deployment target in the repository) | - |

## Real user surfaces

HTTP endpoints listed in README; the acceptance feature exercises approval, decline, blocked card, idempotent
retry, 16 concurrent purchases, risk timeout (< 1000 ms decision), capture, reversal and transaction listing.

## Missing harnesses (Observed)

No load or latency test against the 200-300 ms budget beyond the single timeout scenario; no Redis outage
integration test (covered by a mocked unit test only); no contract test against a real risk engine.
<!-- END project-context-initializer:artifact -->
