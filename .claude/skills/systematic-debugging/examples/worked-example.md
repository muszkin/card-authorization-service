# Worked example: export test fails only in the full suite

> Illustrative only. The paths, timings and code in this example are invented to
> show the shape of a completed investigation. Never reuse a finding from this
> file as evidence about a real repository.

## Report intake

- **Expected:** `export emits one row per invoice` passes in CI.
- **Actual:** fails roughly one run in three with `expected 41 rows, got 40`.
- **Observed by:** CI on the integration branch, first seen 2026-08-11.
- **Revision:** fails on several revisions including ones that passed earlier, so it is not obviously a regression.
- **Raw evidence:** `ci/run-8841/export.log`, assertion diff retained.
- **Reporter's diagnosis:** "the export query is flaky." Recorded as a hypothesis, not an observation.

Known-flaky list checked: the test is not on it. No prior investigation found.

## Reproduction

Alone: passed 20 of 20 runs. In the full suite: failed 7 of 20.

That difference is the finding, so the reproduction is the suite, not the test. Controlled starting state recorded: same seed, leased schema, single worker, fixed clock.

```text
Base revision:   9f2c1ab
Setup:           npm run db:reset && npm run seed -- --fixture billing
Reproduction:    npm test -- --runInBand
Expected:        41 rows
Actual:          40 rows, intermittently
Reliability:     7 failures in 20 runs
Evidence:        evidence/suite-runs/*.log
```

Not the harness: the suite starts cleanly, the fixture loads, and the failure is an assertion on real output.

## Classification

`flaky-or-environmental` was the reporter's implied classification. Rejected: the failure rate depends on which other tests run, which is a deterministic dependency, not environmental noise. Reclassified as a product or test defect involving shared state.

## Narrowing

| # | Variable changed | Prediction | Result | Ruled out |
| --- | --- | --- | --- | --- |
| 1 | Ran export test alone, 20x | Passes if suite-dependent | Passed 20/20 | The test in isolation |
| 2 | Randomized suite order, 20x | Failure rate changes if order-dependent | 3/20, different order each time | Fixed-order-only causes |
| 3 | Bisected the case list around the failure | One other case is required | `invoice retention` present in every failing run | All other cases |
| 4 | Ran only `invoice retention` then export, 10x | Fails every time if that pair is sufficient | Failed 10/10 | Any third case being needed |
| 5 | Same pair with the clock unfrozen | Passes if the frozen clock matters | Still failed 10/10 | Clock handling |

Minimal case: `invoice retention` followed by `export`, reliably failing.

## Hypothesis and experiment

**Hypothesis:** the retention test deletes an invoice inside its own transaction, the export test's fixture count assumes 41 invoices, and retention's cleanup does not restore the deleted row, so export legitimately finds 40.

**Prediction:** counting invoices between the two tests will show 40, and restoring the row will make export pass while leaving retention passing.

**Result:** the count between tests was 40. Restoring the row made export pass 10 of 10.

Hypothesis supported. One earlier hypothesis, that export's query missed a row under concurrent writes, was disproved at experiment 4: with a single worker there is no concurrency and the failure still occurred.

## Root cause

The retention test calls the production deletion path directly (`src/billing/retention.ts:52`) rather than through its test helper, so its cleanup hook — which only truncates tables it created — never restores the shared fixture invoice.

Why it did not surface earlier: the two tests were in different files that previously ran in different shards. The shard split changed on 2026-08-08, three days before the first report, which also explains the intermittency: only some orderings place them in the same shard.

The reporter's diagnosis was wrong in a specific way worth recording. The export query was correct throughout; the fixture it counted was not.

## Regression proof

Added `test/billing/retention.fixture.test.ts`, asserting the shared fixture row count is unchanged after the retention suite.

Proven RED at `9f2c1ab` before any fix: failed with `expected 41, got 40`, output retained at `evidence/regression-red.log`. Passes after the fix.

## Handoff

- **Classification:** product-test defect in shared fixture ownership, not a product defect in export.
- **Recommended profile:** `quick-bug-fix`. The change is one test file plus its helper, no product code, low blast radius, and the reproduction is deterministic.
- **Residual risk:** other tests may call production deletion paths directly. Not investigated here; raised for closeout as a candidate lesson.
- **Not changed:** the export query, despite being where the failure surfaced.
