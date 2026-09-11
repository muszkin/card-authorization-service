# S2 packet — invalid monetary input returns400

Assigned worktree:/tmp/zilch-20260911-s2
Branch:fix/safe-numeric-boundaries-s2
Parent:3587d2fec48a4956d57bd785840f98d930d750a3
Plan:/tmp/zilch-20260911-feature/context/plans/2026-09-11-safe-numeric-boundaries.md
Plan SHA:cd73e243f2f7ed299e7c3d456a3d56c3af2fc4a5bce91401dac2630eafaa79ef
Contract:bug / fire-and-forget / integration-merged; worker gpt-5.6-terra medium.

Implement S2 only after orchestrator dispatches. Read root CLAUDE, domain/adapter CLAUDE and scoped contexts,
acceptance context, DECISIONS, AI_USAGE and .claude/skills/README.md. S1 exact risk conversion must remain intact.
Own Money.java, MoneyTest.java, AuthorizationControllerTest.java, CardControllerTest.java,
existing Cucumber CardAuthorizationSteps.java/card_authorization.feature, AI_USAGE.md. No other product/build files.
Do not change central run state, push/merge or spawn agents. Diagnostic tests for Money+controllers are available in
/tmp/zilch-20260911-numeric-diagnosis/src/test/java/pl/fairydeck/authorization/ and may be reused selectively.

Acceptance: Money.of rejects values outside long minor-unit bounds with IllegalArgumentException retaining cause;
GBP92233720368547758.07 and lower long boundary remain exact; overflow on each side rejected.
Existing currency exponent and positivity behavior unchanged; plus/minus overflow stays ArithmeticException.
Both POST/v1/authorizations amount and POST/v1/cards creditLimit oversized ->400 application/problem+json.
No authorization/hold/card persisted. MVC no-use-case-interaction assertions and public transactions/balance queries
can prove side effects without adding persistence APIs or broad DB test hooks.
Do not globally handle ArithmeticException or mask unrelated programming errors. Catch narrowly at Money input conversion.

AI_USAGE: correct stale73-78 wording: complete seven-skill bundle is vendored under .claude/skills (see README);
closeout was used in original delivery, later settlement fix used orchestrated workflow. Preserve truthful history.
Opening should distinguish initial Claude Code implementation from subsequent Codex numeric-boundary repair.
Any new current-run statement must describe already-observed steps, not claim future review/CI/merge done.
A concise link to this run can carry changing delivery evidence. No invented claims or extra process prose.

Prove focused RED and Cucumber RED before production edit. Do not weaken assertions to manufacture GREEN.
Record test artifact hashes with RED. Commit named tests using test:, then minimal Money/AI changes using fix:/docs:.
Run ./gradlew test --tests '*MoneyTest' --tests '*AuthorizationControllerTest' --tests '*CardControllerTest'.
Static: ./gradlew compileJava compileTestJava compileTestFixturesJava compileIntegrationTestJava compileAcceptanceTestJava
and ./gradlew test --tests '*ArchitectureTest'. Inspect diff/introduced dependencies/secrets. Sonar NOT_APPLICABLE.
STOP at immutable candidate and static GREEN for independent review; post-review E2E remains NOT_RUN for orchestrator.

Runtime lease:S2 exclusive when dispatched. Existing random app/WireMock ports and fresh Testcontainers, no Compose.
Record effective ports/container evidence and verify owned runtime teardown. Guard path/branch/HEAD/status before gates/commit.
Report/evidence only under /tmp/zilch-20260911-feature/context/implementation-runs/20260911-safe-numeric-boundaries/slices/S2/.
Return candidate+RED SHAs, intended files, exact tests/exits/counts/logs and root cause/residual risks.
