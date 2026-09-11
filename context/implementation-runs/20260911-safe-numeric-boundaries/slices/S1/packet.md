# S1 packet — safe risk response

Assigned worktree:/tmp/zilch-20260911-s1
Branch:fix/safe-numeric-boundaries-s1
Parent:73f5ee0d94ff1d13c5e5f22b6d73604aaf133fa3
Plan:/tmp/zilch-20260911-feature/context/plans/2026-09-11-safe-numeric-boundaries.md
Plan SHA:cd73e243f2f7ed299e7c3d456a3d56c3af2fc4a5bce91401dac2630eafaa79ef
Contract:bug / fire-and-forget / integration-merged; worker gpt-5.6-terra medium. No model fallback/profile downgrade.

Implement S1 only. Read root CLAUDE, context/map/INDEX.md, adapter CLAUDE and .agents context, acceptance context.
Own HttpRiskScorer.java, HttpRiskScorerTest.java and existing Cucumber CardAuthorizationSteps.java/card_authorization.feature.
Do not change other product/docs/build files or central run state; no push/merge/review spawn.
Diagnostic test available in /tmp/zilch-20260911-numeric-diagnosis/src/test/java/pl/fairydeck/authorization/adapter/out/risk/HttpRiskScorerTest.java.
Diagnostic report in this run diagnosis.md. Base clean build136 tests PASS.

Acceptance: exact integers70,70.0 preserve Scored70;70.9,missing,null,int overflow,malformed JSON ->Unavailable.
Full HTTP: fractional risk ->201 DECLINED/RISK_UNAVAILABLE with unchanged available balance; valid purchase preserved.
Preserve existing int range semantics; no invented0..100 rule. Prefer BigDecimal wire score+intValueExact local conversion;
no floating point, rounding, global Jackson config or broad RuntimeException catch.

Prove focused RED and Cucumber RED before production edit. Record exact scenario/assertion and test SHA256.
Commit named regression tests using test: after RED, then fix: after focused GREEN; no amend/squash.
Run ./gradlew test --tests '*HttpRiskScorerTest', then compileJava compileTestJava compileTestFixturesJava
compileIntegrationTestJava compileAcceptanceTestJava plus test --tests '*ArchitectureTest' on candidate.
Sonar NOT_APPLICABLE (not configured); inspect diff for secrets/dependency changes.
STOP after immutable candidate and static GREEN. Independent reviewer and post-review E2E run by orchestrator.
Full post-review E2E command ./gradlew acceptanceTest --rerun-tasks remains NOT_RUN, never PASS.

Runtime lease:S1 exclusive, ACQUIRED. Only this worker may run tests currently. Use existing random app/WireMock ports
and fresh Testcontainers. No Compose/shared services. Record effective test port/container evidence from logs,
verify owned processes/containers gone after tests. Before every gate/commit verify path,branch,HEAD,status.

Write concise worker-report.md and raw evidence only under
/tmp/zilch-20260911-feature/context/implementation-runs/20260911-safe-numeric-boundaries/slices/S1/.
Do not commit those central copies from your checkout. Return candidate SHA, RED commit, commands/exits/log paths,
root cause, test counts, intended diff and residual risks.
