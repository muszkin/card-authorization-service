# S2 independent review

Verdict: **PASS**

Reviewed immutable candidate: `5debbf647734dadcae75960f405ba5602a9bc82b`  
Base / merge-base: `3587d2fec48a4956d57bd785840f98d930d750a3`  
Plan: `context/plans/2026-09-11-safe-numeric-boundaries.md` (`cd73e243f2f7ed299e7c3d456a3d56c3af2fc4a5bce91401dac2630eafaa79ef`)

## Scope and intent

Observed change scope is the seven S2-owned paths: narrow monetary-input conversion, unit/MVC/Cucumber regression coverage, and AI-use provenance. The intent is reviewable: values that cannot be represented as `long` minor units return the existing 400 problem response at both REST entrypoints without business-side effects, while internal arithmetic overflow remains an `ArithmeticException`.

The candidate is a clean detached checkout at the recorded head. `BASE...HEAD` contains only three commits (`test: cover unrepresentable monetary input`, `fix: normalize monetary input overflow`, and `docs: correct AI workflow provenance`); `git diff --check` passed.

## Findings

None. No blocking or non-blocking findings.

## Required-dimension review

| Dimension | Verdict | Evidence |
| --- | --- | --- |
| Acceptance correctness | complete | `Money.of` catches only the `ArithmeticException` produced while converting the external decimal to minor units, retaining it as the cause of a useful `IllegalArgumentException`. Both request records call this method before their use cases; the existing advice maps that exception to 400 problem JSON. |
| Regression / logical correctness | complete | Exact GBP `Long.MAX_VALUE` and `Long.MIN_VALUE` boundaries are covered. `plus` and `minus` remain unchanged `Math.addExact` / `Math.subtractExact` paths and their overflows are asserted as `ArithmeticException`. No global arithmetic handler was added. |
| Edge cases and side effects | complete | Unit coverage verifies both overflow directions. MVC coverage verifies both POST endpoints return 400 problem JSON and make no use-case interaction. The Cucumber scenario verifies a real HTTP 400 for each endpoint and that the pre-existing card's balance and transactions remain unchanged. |
| Security, privacy, data, concurrency, operability | not-applicable | This is a local input-conversion/error-mapping change with no auth, persistence schema, concurrency, telemetry, configuration, or dependency change. Diff inspection found no common secret marker. |
| Architecture | complete | The conversion remains in the money domain and REST mapping remains in the existing REST advice; ArchUnit passed. |
| Tests and E2E strength | complete | New assertions would fail on the parent behavior: the supplied RED focused run has four failures and Cucumber RED fails at the required 400 assertion. Candidate focused unit/MVC and architecture commands pass. Post-review acceptance remains intentionally reserved for the orchestrator and was not run here. |
| Scope and maintainability | complete | Seven changed paths match S2 ownership. AI_USAGE claims were checked against `.claude/skills/README.md` and the seven vendored `SKILL.md` files; the repair wording links to observed run evidence and does not claim future review, CI, or merge completion. |

## RED evidence verification

Verified artifact hashes match the worker report:

- `focused-red.log`: `c8dc1cb248ad604da904387af1228debdc8116b1d74b775b37770a1e407fd2a4`
- `acceptance-red.log`: `686911974a12fba75d951a12326b2d550eb03b13731d0e82ca9d49b5a6e77341`
- The five stated post-RED test/feature hashes match the candidate files exactly, including `MoneyTest.java` `70834d19e9faacc0d3305b3537352468e47e1ca40611e00243511b572c4e7c08` and the feature `22dc48db202a3a75db13faf81d00a7efb311d580cdc955f31ff6e5685cff6290`.

## Commands run by reviewer

```text
git diff --check BASE...HEAD
./gradlew test --tests '*MoneyTest' --tests '*AuthorizationControllerTest' --tests '*CardControllerTest'
./gradlew test --tests '*ArchitectureTest'
sha256sum S2 evidence and candidate RED-test files
```

All executed commands passed. The acceptance/integration suites were not run, per the runtime-reservation contract; the next owner is the orchestrator for post-review acceptance.

## Delivery

This report is the authorized local review artifact only; nothing was posted externally. Review is bound to base `3587d2fec48a4956d57bd785840f98d930d750a3` and head `5debbf647734dadcae75960f405ba5602a9bc82b`.
