# Implementation Plan Template

Use the project's established planning format when it contains the same information. Otherwise adapt this template to the user's and project's language.

Do not leave placeholders in a plan marked `ready-for-approval`. Omit irrelevant sections instead of filling them with generic text.

```markdown
# <Outcome> Implementation Plan

**Status:** draft | ready-for-approval | approved
**Date:** YYYY-MM-DD
**Request:** <issue, user request, or source context>
**Plan location:** <path and whether it is tracked by version control>

## Outcome

<One paragraph: actor, problem, observable result, and why it matters.>

## Scope

### In scope

- <Concrete behavior>

### Out of scope

- <Adjacent behavior intentionally excluded>

## Evidence map

| Evidence | What it establishes | Provenance |
| --- | --- | --- |
| `<repo path, symbol, command, runtime trace, issue, or official source>` | <fact used by the plan> | Observed / User-confirmed / Inferred / Unknown / Contradiction |

When a generated project-context index is resolved from active instructions or the default `context/map/INDEX.md`, include its manifest path, source snapshot, and freshness verdict in this map, then cite the current primary files, symbols, commands, or runtime evidence that confirm every consequential claim used by the plan. Generated context is a router, not a substitute for current evidence.

## Current state

<Trace the current user or system flow and identify reusable components, constraints, and gaps.>

## Decision and assumption ledger

| Item | Provenance | Decision state | Evidence or rationale | Consequence |
| --- | --- | --- | --- | --- |
| <decision, assumption, or scope boundary> | Observed / User-confirmed / Inferred / Unknown / Contradiction | N/A / Proposed / Undecided / Decided / Accepted risk / Out of scope | <source or user decision> | <effect on the plan> |

## Socratic challenge ledger

| Risk rank | Assumption | Strongest counterexample or failure mode | Evidence | Consequence if false | Decision and accountable owner |
| --- | --- | --- | --- | --- | --- |
| <high / medium / low> | <assumption> | <credible challenge> | <source or missing evidence> | <impact> | <confirmed, revised, rejected, blocked, or accepted risk with owner> |

## Solution contract

### Actors and permissions

<Who can do what and under which conditions.>

### Happy path

1. <Actor action and visible system response>

### Edge cases and failure behavior

| Case | Required behavior | Recovery or fallback | Slice |
| --- | --- | --- | --- |
| <invalid input, empty state, duplicate, retry, concurrency, timeout, partial failure, offline, denied permission, abuse case> | <observable response> | <how actor or system recovers> | S<n> |

### Data, state, and contracts

<State transitions, schemas, public interfaces, integrations, compatibility, idempotency, and preserved behavior.>

### Threat model

Required when the change touches authentication, authorization, secrets or credentials, user-supplied input reaching a sink, payments, personal or regulated data, file uploads, background jobs acting on user data, or a public network boundary. When no trigger applies, replace this section with one line naming the triggers checked and why none apply.

| Asset | Threat | Entry point | Existing control | Gap | Slice that closes it |
| --- | --- | --- | --- | --- | --- |
| <what an attacker wants> | spoofing / tampering / repudiation / information disclosure / denial of service / elevation of privilege | <surface, parameter, or integration> | <control already in place, with evidence> | <what is missing> | S<n> or accepted risk with owner |

- **Trust boundaries crossed:** <where data or control moves between differently trusted parties>
- **Secrets introduced or moved:** <key names and where they are defined; never values>
- **Abuse cases promoted to acceptance:** <which rows become a named test in which slice>

### Non-functional constraints

- <Measurable performance, security, privacy, accessibility, reliability, or operability requirement>

### Rollout and rollback

<Migration order, compatibility window, feature flag, telemetry, deployment sequencing, and safe reversal.>

## Technology decisions

| Decision | Version or policy | Requirement driving it | Rationale | Rejected alternatives | Source |
| --- | --- | --- | --- | --- | --- |
| <technology or existing-stack decision> | <exact or bounded version> | <requirement> | <why> | <why not> | <manifest or official current docs> |

## Global implementation constraints

- <Project instruction, compatibility invariant, naming rule, test rule, or explicit non-goal that every slice inherits>

## Execution topology and gate contract

| Concern | Repository-proven value | Evidence | Execution consequence |
| --- | --- | --- | --- |
| Approved base and integration target | <branch/ref and pinned-base rule> | <docs/config/history> | <where feature integration starts and merges> |
| Feature branch and merge policy | <naming plus rebase/squash/merge policy> | <repo instructions> | <how slices integrate> |
| Required PR checks | <check names or discovery rule> | <branch protection/CI> | <what must pass on current PR SHA> |
| Staging trigger and revision proof | <branch/workflow/promotion plus SHA/digest source> | <deploy docs/workflow> | <how staging is deployed and verified> |
| Production target | <branch/promotion/approval object> | <release docs/workflow> | <terminal production-PR handoff; no inferred deploy authority> |
| Focused tests | `<command>` | <script/config> | <working directory and prerequisites> |
| Static analysis | `<command>` | <script/CI/config> | <working directory and generated-output rules> |
| Dependency, secret and license scanning | `<command>` / none configured | <config/CI/hooks> | <diff-level secret and introduced-dependency checks apply even with no scanner> |
| Sonar | `<scanner and Quality Gate lookup>` / CI-only / not configured | <Sonar config/CI> | <unique worktree key, credentials and deferred gate if applicable> |
| Real-surface E2E | `<headed UI/device or CLI command>` | <test docs/config> | <environment, setup and evidence> |
| Full feature verification | `<commands>` | <CI/test docs> | <post-slice and pre-PR combined gates> |

### Worktree resource isolation

| Resource | Isolation or serialization rule | Evidence |
| --- | --- | --- |
| <one row per applicable resource from the canonical isolation contract> | <unique lease rule or why the resource must be serialized> | <project config/docs> |

### Authorization boundaries

<Name the expected local implementation, remote PR, integration merge, staging deployment/test-data, production-PR, migration/backfill/cutover, and production merge/deploy boundaries. The plan documents these operations but does not authorize them.>

## Slice map

| ID | Actor capability | Prerequisites | Parallel group | Owned risks and edge cases | Verification verdict / primary proof |
| --- | --- | --- | --- | --- | --- |
| S1 | The actor can ... | none | A | <named risks> | <real user flow> |

## Vertical slices

### S1 - <Actor capability>

**Why now:** <requirement or risk>
**Prerequisites:** <merged slices or external conditions>
**Parallel-safe with:** <slice IDs or none, with conflict check result>

#### Boundaries

- **In scope:** <behaviors and edge cases>
- **Out of scope:** <guardrails>
- **Likely change surface:** `<paths, modules, routes, schemas, components>`
- **Conflict footprint:** <shared files, migrations, fixtures, state, generated artifacts>
- **Worktree resource lease:** <resources this slice leases from the canonical isolation contract, each unique or explicitly serialized>

#### Contracts

- **Consumes:** <existing interfaces and invariants>
- **Produces:** <public behavior or exact contract available to later slices>

#### Behavior and acceptance

1. <Numbered happy-path action and visible response>

- <Binary acceptance criterion>
- <Owned edge-case criterion>

#### Test cycle

- **Verification verdict:** real-surface E2E / consumer-boundary E2E / strongest non-E2E evidence / blocked
- **Expected initial RED:** <the missing behavior assertion and evidence that distinguishes it from harness failure>
- **Focused tests:** <specific test cases tied to risks>
- **Static analysis:** `<confirmed repository command>` and working directory
- **Sonar:** `<confirmed scanner plus actual Quality Gate lookup>` / deferred to named CI check / not configured with evidence
- **Independent review focus:** <regressions, logic, edge cases, security/data/concurrency, architecture/ADR and test-strength risks specific to this slice>
- **E2E setup:** <environment, seed, roles, cleanup>
- **E2E flow:** <navigate/click/type/gesture in headed mode by default for local UI, or launch CLI and interact; state any headed-mode exception>
- **Expected evidence:** <visible output, state, screenshot, logs>
- **Deliberate-break check:** <proportionate mutation or failure proof, or why it is not needed>
- **Commands:** `<confirmed focused test command>`, `<confirmed static command>`, `<confirmed Sonar command/disposition>`, and `<confirmed E2E command>`; label a new command as introduced by this slice
- **Post-merge integration check:** <exact feature-branch command and cross-slice contract/user journey>
- **Exception or blocker:** <for non-E2E evidence, reason, residual risk, accountable owner, closing condition, and plan to establish E2E; otherwise omit>

#### Delivery safety

- **Observability:** <signal>
- **Rollout:** <sequence or flag>
- **Rollback/fallback:** <safe reversal>

#### Delivery stages

| Packet | Stage | Implementation scope | Actor-visible change | Compatibility invariant | Operational verification | Rollback | Entry and exit conditions | Required authorization |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| S1-D1 | expand / backfill / cutover / contract | <code, data, or configuration boundary> | <none until cutover, unless designed otherwise> | <behavior that must remain valid> | <proof> | <safe reversal> | <conditions> | <implementation, deploy, migration, backfill, cutover, or cleanup approval owner> |

Omit this table when the capability can ship atomically.

For each delivery-stage row, add a short packet containing exact in-scope and out-of-scope work, focused tests, operational proof, rollback, and the authorization required before execution. Each packet must fit one implementation and review cycle.

#### Executor handoff (subagent-ready)

> Implement only S1, or only the named S1 delivery-stage packet when one is separately assigned and authorized. Read the cited project instructions and evidence. Preserve the global constraints and contracts above. Do not implement later slices or refactor unrelated areas. Do not deploy, migrate, backfill, cut over, or remove compatibility paths without separate explicit authorization for that operation. Run the focused tests and the strongest verification allowed at this stage, then report changed files, commands, results, retained evidence, and any plan assumption disproved by implementation.

<Repeat for each slice.>

## Dependency and concurrency audit

<DAG or ordered list. Explain why each concurrency group has no contract, conflict-footprint, generated-output, migration, or runtime-resource collision. State the required topological integration order.>

## Implementation-orchestrator handoff

- **Canonical plan identity:** <absolute/repository path, safe SHA-256, source revision, and current approval status>
- **Project-context entrypoint:** <resolved context index/manifest plus snapshot/freshness verdict>
- **Execution DAG:** <slice IDs, dependencies, proven parallel cohorts, integration order>
- **Feature integration:** <base/target branches, feature branch convention, merge policy, post-slice checks>
- **Gate coverage:** acceptance/regression proof -> focused checks -> static -> Sonar Quality Gate -> independent review -> real-surface E2E; this complete final coverage is mandatory regardless of later execution cadence
- **Final delivery:** <full feature gates, integration PR/required CI, staging revision proof/E2E, production-target PR boundary>
- **Expected authorization envelope:** <operations implementation must confirm once before execution>
- **Reconciliation record:** <path/hash/producer/source revision for federation by project-context-initializer>

## Execution recommendation

This section is a recommendation before plan approval, not an implementation decision.

| Decision | Recommendation | Evidence and trade-off |
| --- | --- | --- |
| Profile | quick-dev / quick-bug-fix / standard / bug | <scope, bug/non-bug, quick eligibility or exclusion, risk and expected gate cadence> |
| Orchestration | fire-and-forget / supervised | <why autonomous delivery or named checkpoints fit> |
| Terminal outcome | local-green / ready-pr / integration-merged / staging-green / production-pr-ready / production-green | <exact observable done state and required authority> |
| Model policy | daily-coding defaults / override | <Codex or Claude worker/reviewer model, reasoning effort and availability caveat> |
| Automatic escalation | allowed / confirm first | <quick-dev -> standard; quick-bug-fix -> bug or standard triggers> |

### Quick-profile eligibility

- **Verdict:** eligible / not eligible
- **Evidence:** <bounded change surface, healthy baseline, isolated ownership, rollback and lack/presence of protected or high-risk boundaries>
- **Per-slice cadence if quick:** <focused regression/acceptance tests and cheap build/type/static checks>
- **Deferred to final:** <Sonar, independent review and real-surface E2E obligations, each explicitly named; none is called green>
- **Escalation triggers:** <migration, public contract, auth/security/privacy/payment, infrastructure/deployment, cross-service, concurrency/data integrity, expanding footprint or unclear reproduction>

### Operator execution decision

**Status:** pending-plan-approval | awaiting-operator | confirmed

- **Selected profile:** <value>
- **Selected orchestration:** <value>
- **Terminal outcome:** <value and exact proof>
- **Selected worker/reviewer model policy:** <requested values; implementation resolves availability>
- **Automatic profile escalation:** <value>
- **Challenge raised:** <evidence-based objection and recommendation, or none>
- **Operator override:** <decision and accepted consequence, or none>
- **Implementation authorization:** <trusted instruction and exact envelope, or not yet granted>

## Coverage matrix

| Requirement, happy-path step, edge case, or risk | Slice | Focused test | Verification evidence |
| --- | --- | --- | --- |
| <item> | S<n> | <test> | <observable proof> |

## Accepted risks

- <Risk, evidence, owner, mitigation, and trigger to revisit>

## Approval

<First request plan approval. After approval, request the single compound operator execution decision above immediately before implementation begins.>
```

## Plan-writing rules

- Keep WHAT in the solution contract and HOW in the slice packets.
- Include exact public contracts when sibling slices depend on them. Do not prescribe full implementation code unless syntax itself is a compatibility requirement.
- Use one canonical progress state. Do not duplicate conflicting checkbox systems across sections.
- Cite real paths and commands discovered in the project. A generic command is not an exact verification plan.
- Distinguish commands already present from commands a slice will introduce, and distinguish a Sonar scanner run from the server-side Quality Gate result.
- Map every edge case and accepted risk to ownership. Every material accepted risk requires explicit acceptance by the user or a named accountable owner. Unowned risk is an unresolved blocker.
- If a plan path is ignored by version control, state that beside `Plan location` and in the handoff.
- `ready-for-approval` means the audit passed; it does not mean implementation may start.
- The execution recommendation must not weaken full final gate coverage. A quick profile changes when named gates run, not whether they run.
- Do not treat the recommended execution contract as selected. Only the operator decision can set it to `confirmed` and authorize implementation within its recorded envelope.
