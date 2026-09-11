# Safe numeric boundaries for authorization

Status: implementation scope authorized by the user's 2026-09-11 instruction to fix F1, F2 and AI_USAGE.md through the full workflow; execution contract confirmed: bug, fire-and-forget, integration-merged.
Source: review of 2156da5; target base d7537cdb2b25fbf6a6b4ec7bed4dbddd8bf274fb (origin/main).
Language: English, following the repository documentation convention.

## Outcome and boundaries

A caller receives a safe authorization decision when external scoring contains an inexact number,
and a useful 400 problem response when an input amount cannot fit the existing minor-unit representation.
Existing valid purchases, credit limits, currencies, idempotency, settlement and ledger behavior remain unchanged.
AI_USAGE.md accurately distinguishes the original Claude Code delivery, the bundled toolkit, and this Codex repair.

No new dependency, endpoint, schema, global JSON policy, score business range, timeout promise, authentication,
ledger arithmetic behavior, deployment, or unrelated ADR rewrite. In particular, do not map all ArithmeticException
to HTTP 400. No history rewriting or squash.

## Evidence and current flow

- Observed: HttpRiskScorer.ScoreResponse(int) permits Jackson coercion of 70.9 to 70; AuthorizationPolicy declines only scores above 70.
- Observed: Money.of uses longValueExact; overflow escapes as ArithmeticException, while ApiExceptionHandler maps IllegalArgumentException to 400.
- Observed: AuthorizationRequest.toPurchase and IssueCardRequest.limit both consume Money.of; both HTTP callers need regression proof.
- Observed: AI_USAGE.md says the other skills are described rather than copied, but .claude/skills contains the workflow bundle.
- Observed: the clean baseline build at d7537cd passed on 2026-09-11; fresh diagnosis retains failing tests before product edits.
- Observed: origin/main differs from the reviewed snapshot only in .github/workflows/build.yml (self-hosted, home).
- Context: context/map/INDEX.md and manifest.json are historical navigation; direct affected-source reads establish current behavior.
  A bounded initializer refresh will reconcile commands, counts, this plan and the run before final gates.

## Decisions and adversarial challenge

| Assumption / alternative | Counterexample | Decision |
| --- | --- | --- |
| An int wire field validates integral scoring | 70.9 becomes 70 and may approve | Parse without precision loss and convert exactly at the risk boundary |
| Any decimal syntax is invalid | 70.0 is exactly 70 and previously worked | Preserve mathematically integral values; reject nonzero fractional parts |
| Add a score range of 0..100 | No real external contract establishes that range | Preserve the existing int domain; no invented business rule |
| Catch every arithmetic exception in REST | An internal ledger calculation defect would become400 | Normalize only input conversion failure in Money.of |
| Only purchase amounts consume Money.of | Issuing a credit limit shares the same conversion | Prove both HTTP entrypoints |
| Unit tests suffice | Boot's mapper and HTTP exception handling may differ | Add Cucumber consumer-boundary proof with actual app/Postgres/Redis |
| Parallel slices are cheaper | Both edit the same Cucumber feature and steps | S1 then S2, serialized runtime |

Read-only independent challenge confirmed the narrow boundary changes and rejected global mapper/exception changes.

## Contract and threat model

Triggers: payments, external HTTP response, public user-supplied numeric input.

| Asset / boundary | Threat or failure | Acceptance / owner |
| --- | --- | --- |
| Available credit / risk response | Fraction truncation lowers risk and creates an unintended hold | S1: 70.9 -> 201 DECLINED/RISK_UNAVAILABLE; balance unchanged |
| Risk domain / external response | Null, missing, malformed or int-overflow score | S1: Unavailable, no uncaught exception; valid integer and 70.0 preserved |
| API availability / amount and creditLimit | Representational overflow produces server error | S2: 400 application/problem+json; no authorization/hold, no card issued |
| Internal money arithmetic | Broad catch hides programming failures | S2: plus/minus continue throwing ArithmeticException on overflow |

No secrets or new personal data introduced. External risk engine remains WireMock because this is an API-only demo
with no production engine endpoint. Internal persistence and cache are real Testcontainers instances.
Valid boundary: GBP 92233720368547758.07 fits Long.MAX_VALUE minor units; the next cent does not.
Currency exponent behavior (GBP, JPY, BHD), positivity and existing error/status contracts stay unchanged.
No latency or throughput improvement is claimed. No stored data changes; rollback is reverting the fix commits,
which restores the known defects and therefore needs a conscious delivery decision.

## Technology and topology

Retain Java 25, Spring Boot 4.1.1, Gradle 9.7.1, Jackson 3 and existing test libraries from build.gradle.kts/wrapper.
Use existing BigDecimal exact conversion, no dependency/version changes or research-driven technology decision.
Base/target: origin/main d7537cd; feature fix/safe-numeric-boundaries. Local integration: fast-forward only;
remote merge (if separately selected): merge commit preserving the test/fix sequence, no squash/amend/force-push.
CI: .github/workflows/build.yml, build job, self-hosted+home labels, PRs and main pushes. Branch protection API
reports unprotected main; the workflow build remains a required delivery check by this plan.
No staging/production/deploy workflow exists. Sonar, dependency/license/secret scanner: not configured;
Sonar NOT_APPLICABLE, introduced-dependency check NOT_APPLICABLE (no additions), diff-level secret check required.

## Sequential slices and ownership

### S1 — Caller receives a fail-closed decision for inexact scoring

Prerequisite: baseline/diagnostic evidence. No parallel group.
Own: src/main/java/pl/fairydeck/authorization/adapter/out/risk/HttpRiskScorer.java;
src/test/java/pl/fairydeck/authorization/adapter/out/risk/HttpRiskScorerTest.java;
src/acceptanceTest/{java,resources}/pl/fairydeck/authorization/acceptance/{CardAuthorizationSteps.java,card_authorization.feature}.
The two acceptance paths mean their respective existing java/resource locations, not new directories.

Happy path: issue card -> purchase with valid score -> 201 APPROVED and exactly one hold.
Regression: engine answers70.9 -> 201 DECLINED/RISK_UNAVAILABLE, available balance unchanged.
Focused cases:70,70.0,70.9, missing/null score, malformed JSON, int overflow; retain existing timeout/5xx behavior.
RED: unchanged production adapter yields Scored(70), and real HTTP approves the fractional-risk purchase.
Implementation approach: preserve exact external numeric value until checked conversion to the existing int model;
reject missing/unrepresentable values locally and reuse Unavailable. Do not catch arbitrary RuntimeException.
Focused command: ./gradlew test --tests '*HttpRiskScorerTest'.
E2E command: ./gradlew acceptanceTest --rerun-tasks (API is the real consumer surface).
Review focus: exactness, null, error handling scope, Boot mapper fidelity, unchanged successful scoring.

### S2 — Caller receives 400 for an unrepresentable amount or credit limit

Prerequisite: S1 integrated; shared acceptance files require sequencing.
Own: src/main/java/pl/fairydeck/authorization/domain/money/Money.java;
src/test/java/pl/fairydeck/authorization/domain/money/MoneyTest.java;
src/test/java/pl/fairydeck/authorization/adapter/in/rest/{AuthorizationControllerTest,CardControllerTest}.java;
same existing Cucumber feature/steps; AI_USAGE.md.

Happy path: representable money including the maximum long minor-unit amount is parsed exactly.
Regression: oversized authorization amount and issued-card credit limit ->400 problem JSON, no persisted side effect.
Focused cases: overflow on both sides of long bounds in Money.of, representable boundaries, currency exponent preservation;
internal plus/minus overflow stays ArithmeticException. No broad global arithmetic handler.
RED: Money.of throws wrong exception type; MVC/HTTP does not produce expected 400.
Focused command: ./gradlew test --tests '*MoneyTest' --tests '*AuthorizationControllerTest' --tests '*CardControllerTest'.
E2E command: ./gradlew acceptanceTest --rerun-tasks.
Review focus: only parsing failures normalized, both REST callers, useful validation message, precise AI provenance.
AI_USAGE update: accurately describe the full vendored bundle and current Codex test/review workflow; cite real evidence,
not predicted success. Do not claim all original work used the later orchestration pipeline.

## Common gate and resource contract

Each worker: verify assigned Git path/branch/parent, read root/layer CLAUDE and scoped context; reproduce before editing;
commit regression tests after observed RED, then fix after GREEN. Only named paths staged. No worker push/merge.
Static: ./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava compileAcceptanceTestJava
and ./gradlew test --tests '*ArchitectureTest'; compiler -Xlint:all,-serial -Werror is already enabled.
Per slice: RED -> focused tests -> immutable candidate -> static/diff checks -> independent detached review -> acceptanceTest -> integration.
Post-integration: ./gradlew test integrationTest. Final: ./gradlew clean build, fresh combined independent review,
then ./gradlew acceptanceTest --rerun-tasks on the same candidate. Any product/test repair invalidates downstream gates.

Resources: one runtime lease at a time across diagnosis/workers/integration/final checks. Existing harness allocates ephemeral
WireMock/app ports and per-process Postgres/Redis containers. Worktree-local build/temp outputs; Gradle user cache is lock-managed.
No Compose, fixed service ports, shared database, broker, browser, accounts or production fixtures. Capture actual port/container
identities from test logs and verify owned containers stop before transferring the lease. CI also uses this host pool: avoid overlapping
local heavy tests once CI begins.

## Execution recommendation and authority

Profile: bug (numeric payment boundaries exclude quick eligibility). Mode: fire-and-forget.
Workers and independent reviewers: gpt-5.6-terra,medium, resolved from host tool model catalog.
Automatic profile escalation: not applicable (already full bug); repair loops remain authorized, no scope expansion.
Terminal: integration-merged. Operator explicitly selected "PR, zielone CI i merge do main". Push, PR, green CI and merge to main are authorized; no deployment is requested.
Current trusted user instruction authorizes fixing F1/F2, AI_USAGE and the full local workflow including isolated branches,
commits, tests, review and evidence. Plan formalizes these known accepted outcomes; no new product behavior requires reconfirmation.

## Coverage and closeout

F1 -> S1 risk tests+Cucumber; F2 -> S2 Money+both MVC tests+Cucumber; AI_USAGE -> S2 independent prose/diff review.
Core regressions ->existing 136 tests, including real ledger concurrency and idempotent settlement.
Durable evidence: context/implementation-runs/20260911-safe-numeric-boundaries/ (run contract, RED, review, gates, delivery).
Initializer owns context/map and scoped-context refresh; orchestrator owns run state. Closeout emits at most the useful
boundary-conversion decision and an accurate workflow record, avoiding duplicate test narration and unrelated instruction rules.
