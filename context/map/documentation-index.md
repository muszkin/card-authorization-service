<!-- BEGIN project-context-initializer:artifact -->
# Documentation index

Affected-scope refresh at `5debbf647734dadcae75960f405ba5602a9bc82b` on 2026-09-11T09:42:24Z. Historical documentation-read evidence remains dated to `b4bef16`. Navigation evidence only: current user instructions, code, tests, runtime behaviour and canonical docs outrank this file.

| Path | Scope | Topic | Authority | Read | Used by |
| --- | --- | --- | --- | --- | --- |
| `README.md` | repository | purpose, run instructions, API, structure, tests, limitations | canonical | full | overview, delivery |
| `DECISIONS.md` | repository | 16 decision records with rejected alternatives | canonical (ADR-like) | full | architecture, risks |
| `AI_USAGE.md` | repository | how the agent was used, what was rejected, verification | canonical (process) | full | overview, git history |
| `CLAUDE.md` | repository | agent working rules and commands | canonical (instructions) | full | routers |
| `src/main/java/pl/fairydeck/authorization/domain/CLAUDE.md` | domain | layer rules | canonical (instructions) | full | domain context |
| `src/main/java/pl/fairydeck/authorization/application/CLAUDE.md` | application | layer rules | canonical (instructions) | full | application context |
| `src/main/java/pl/fairydeck/authorization/adapter/CLAUDE.md` | adapter | layer rules | canonical (instructions) | full | adapter context |
| `src/main/java/pl/fairydeck/authorization/technical/CLAUDE.md` | technical | layer rules | canonical (instructions) | full | technical context |
| `src/acceptanceTest/resources/pl/fairydeck/authorization/acceptance/card_authorization.feature` | acceptance | executable specification (9 scenarios) | canonical (tests) | full | flows, delivery |
| `.github/workflows/build.yml` | CI | build pipeline | canonical (config) | full | delivery |
| `compose.yaml`, `local/risk-engine/mappings/scores.json` | local run | infrastructure and risk stub | canonical (config) | full | delivery |
| `src/main/resources/application.yaml` | runtime | configuration surface (key names in technology/architecture) | canonical (config) | full | architecture |
| `.claude/settings.json` | agent tooling | permissions | canonical (config) | full | discoverability |

No stale, duplicated or orphaned documents found. No `.doc`/`.docx` present.

## Related artifacts from other skills and tools

| Path | Generator / owner | Purpose | Type | Authority | Approval | Freshness | Consumers |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `.claude/skills/task-closeout/SKILL.md` (+ `references/`, `examples/`) | `task-closeout` skill bundle, owner: repository author | close-out workflow: triage lessons into ADR / rule / memory / work log | generated-synthesis (skill definition) | advisory | not-applicable | current at `b4bef16` | task-closeout, review |
| `DECISIONS.md` | repository author (written during delivery) | decision records | canonical-evidence | canonical | approved (committed) | current | planning, review, orchestrator |

| `context/plans/2026-09-11-safe-numeric-boundaries.md` | implementation-planning | approved F1/F2 numeric-boundary and AI provenance repair plan | proposal | supporting | approved | source `d7537cd`; current at the checkpoint | implementation-orchestrator, review |
| `context/implementation-runs/20260911-safe-numeric-boundaries/RUN.md` | implementation-orchestrator | frozen pre-final-gates checkpoint for the approved numeric-boundary repair | historical-record | supporting | not-applicable | source `5debbf6`; current checkpoint, but its pending final gates are historical | review, task-closeout, delivery PR |

The plan and implementation run are preserved execution evidence, not a source of current authorization. The run's final
SHA-bound audit is intentionally external to this candidate and should be read from the delivery PR when available.
<!-- END project-context-initializer:artifact -->
