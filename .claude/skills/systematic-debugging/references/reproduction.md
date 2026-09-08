# Reproduction

A defect that cannot be reproduced cannot be fixed, only guessed at. Reproduction is the gate everything else depends on, so it gets the most rigour.

## Choose the boundary

Reproduce at the highest boundary that still shows the failure, then move inward once it is reliable.

1. **The real user surface** — the browser, desktop, mobile, or terminal interaction the reporter used. Highest fidelity, slowest loop.
2. **The consumer boundary** — the public API, event, CLI invocation, or library call the failing path goes through.
3. **The component boundary** — one module or service exercised directly.
4. **The unit** — the smallest function that still exhibits it.

Starting too low is the common mistake: a unit test that reproduces "a" failure may reproduce a different one than the reporter saw. Confirm the failure at a high boundary first, then descend deliberately, checking at each step that the same failure follows you down.

Never substitute a direct protocol call for a real user interaction when the reported failure involves the interface. Rendering, state, focus, timing, and session behavior do not survive that substitution.

## Establish a controlled starting state

Record and control everything the failure could depend on:

- revision, build, or deployed artifact digest;
- runtime, framework, and dependency versions actually resolved, not declared;
- configuration and feature flags in effect;
- data: the exact fixture, seed, or record, including anything about it that is unusual;
- identity, roles, permissions, and tenancy;
- clock, locale, timezone, and encoding when the failure could involve any of them;
- concurrency: what else was running.

A reproduction that depends on an unrecorded piece of state is not reproducible; it is intermittently lucky.

When reproducing requires starting services, a database or a browser, lease those resources through the orchestrator's [resource isolation contract](../../implementation-orchestrator/references/resource-isolation.md) instead of borrowing whatever is already running. An investigation that competes with another assignment for a port produces evidence about the collision, not about the defect.

## Prove it fails for the reported reason

A failing command is not yet a reproduction. Confirm all of:

- the observed symptom matches the report, not merely the same test turning red;
- the failure is not the harness failing to start, a missing fixture, an unreachable service, an expired credential, or a compilation error;
- the failure survives a clean run from the controlled starting state;
- the failure disappears when the reported precondition is removed, which shows you have the right trigger.

Record the raw failure output, not a summary. The exact message, stack frame, and exit code are what later distinguish this defect from a similar one.

## Reliability

Run the reproduction several times from the controlled state and record how often it fails.

- **Deterministic** — fails every time. Proceed.
- **Intermittent** — fails sometimes. Do not proceed to narrowing until you can raise the rate; treat the nondeterminism as part of the defect and find the variable behind it. Ordering, timing, concurrency, shared state between tests, resource contention, randomized identifiers, and unstable clocks are the usual sources.
- **Never in your environment, real for the reporter** — the difference between the environments is the finding. Compare versions, configuration, data, identity, and platform systematically before concluding it is unreproducible.

An intermittent failure suppressed by a retry is a defect that will return under load, when it is far more expensive.

## Production and shared environments

When the defect only appears in a shared or production environment, gather read-only evidence first: logs, traces, metrics, request identifiers, deployed revision, and the state of the affected records.

Do not restart a service, mutate data, redeploy, or toggle a flag to test a theory unless that exact action is explicitly authorized. Those actions destroy the evidence you need and can convert an investigation into a second incident.

Prefer reconstructing the environment's distinguishing conditions locally over experimenting in it.

## Recording the reproduction

Write it so another person on another machine can run it:

```text
Base revision:   <commit or artifact digest>
Setup:           <exact commands to reach the controlled state>
Reproduction:    <exact command or user actions, with working directory>
Expected:        <what should happen>
Actual:          <what happens, with raw output>
Reliability:     <n failures in m runs>
Evidence:        <paths to logs, traces, screenshots>
```

Keep this record with the investigation. It becomes the specification for the regression test and the proof the defect existed before the fix.

## When it is not reproducible

`NOT_REPRODUCIBLE` is a legitimate result, and reporting it honestly is more useful than a speculative fix.

Report what was tried at each boundary, which environments and data were used, what evidence was unavailable and why, the most likely explanations ranked by remaining evidence, and the concrete thing that would make it reproducible: a fuller log, a specific record, access to an environment, or a reporter's exact sequence.

Do not close it as "cannot reproduce" while an obvious evidence source has not been requested.
