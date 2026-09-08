# Final feature gates

## Attempt 1 — assembled SHA `3ddbb487e14a749b538a3536207c492f96ac9f65`
| Gate | Status | Evidence |
| --- | --- | --- |
| full build (static, ArchUnit, unit, integration, acceptance) | FAIL — acceptanceTest 1/10 failed: first scenario declined after a cold-start risk timeout | `final-build.txt`, `final-acceptance-failure.xml`, `final-gate-failure-001.md` (classified flaky-or-environmental, repaired by R1) |
| Sonar | NOT_APPLICABLE | not configured |
| independent combined review of `37e3d11..80528ab` | FAIL — 2 MEDIUM on the committed run ledger (self-contradictory S1 gates, repair evidence not in tree); code, tests, DECISIONS §17 and README judged correct and plan-conformant | `combined-review.md` |

## Attempt 2 — assembled SHA `316edc28ae2b194ef898c543926845379cc0a1cf`
| Gate | Status | Evidence |
| --- | --- | --- |
| full build (`./gradlew build --rerun-tasks`) | PASS — test 106/0, integrationTest 20/0, acceptanceTest 10/0; warm-up request 341 ms, scenario requests 9-11 ms | `final-build-2.txt` |
| Sonar | NOT_APPLICABLE | not configured |
| dependency / secret scan | PASS | no dependency added; diffs inspected in both reviews |
| independent re-review of the two MEDIUM findings and the docs delta | NOT_RUN at commit time | `combined-review.md` (re-review section) |
| real-surface E2E | inside the full build | as above |
