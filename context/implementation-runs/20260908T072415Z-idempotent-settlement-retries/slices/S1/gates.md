# S1 gates (candidate `ddd71fada0e301552715ec135e4e3366dd328068`)

| Gate | Status | SHA | Command / evidence |
| --- | --- | --- | --- |
| baseline | PASS | 37e3d11 | focused suites green in the slice worktree; `evidence/baseline.txt` |
| acceptance RED | PASS | f0f62f4be99a43b7a388dc1c8689caa342e98c53 | 3 new unit tests fail with `IllegalStateException` (capture on CAPTURED, reverse on REVERSED, reverse on EXPIRED) while siblings pass; acceptance steps compile; `evidence/red-unit.txt` |
| implemented + focused tests | PASS | ddd71fada0e301552715ec135e4e3366dd328068 | `./gradlew test` green in the slice worktree (worker report); deliberate-break reproduced the 3 failures |
| static (`-Xlint:all,-serial -Werror` compile of all source sets + ArchitectureTest) | PASS | ddd71fada0e301552715ec135e4e3366dd328068 | `evidence/static.txt` |
| dependency / secret scan | PASS | ddd71fada0e301552715ec135e4e3366dd328068 | no dependency added (`build.gradle.kts` untouched); diff inspected by orchestrator: no secrets, no machine paths |
| Sonar | NOT_APPLICABLE | — | not configured in build or CI |
| independent review | PASS (3 LOW, non-blocking: line length x2, no application-level mirror test for reverse-after-CAPTURED) | ddd71fada0e301552715ec135e4e3366dd328068 | reviewer-S1 on a detached checkout; `slices/S1/review.md` |
| real-surface E2E (`./gradlew acceptanceTest --rerun-tasks`) | PASS 10/10 incl. "Retrying a capture after it succeeded changes nothing" | ddd71fada0e301552715ec135e4e3366dd328068 | `evidence/e2e.txt`, `evidence/acceptance-results.xml` |
