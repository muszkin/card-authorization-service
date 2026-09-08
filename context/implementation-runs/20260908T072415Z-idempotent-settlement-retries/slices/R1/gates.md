# R1 gates (candidate `80528ab665a5ce44294b69bd90adb60a0051be58`, parent `3ddbb48`)

| Gate | Status | Evidence |
| --- | --- | --- |
| reproduction of the failure | PASS | `integration/final-gate-failure-001.md` (first request 1005 ms, risk I/O error, DECLINED) |
| implemented + focused (compileAcceptanceTestJava, two full `acceptanceTest --rerun-tasks` runs 10/10) | PASS | `evidence/e2e-run-1.txt`, `e2e-run-2.txt`, `acceptance-results-{1,2}.xml`; latencies 385/9/8/10/9/3 and 387/13/10/13/10/3 ms |
| static (`-Werror` compile + ArchUnit) | PASS | `evidence/static.txt` |
| Sonar | NOT_APPLICABLE | not configured |
| independent review | PASS (1 LOW: `warmedUp` check-then-act not thread-safe, latent only under parallel scenarios, which are not configured) | reviewer-final, `integration/combined-review.md`, section "Repair candidate" |
