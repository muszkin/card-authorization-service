# Lesson triage

Most of what happens during a task is not knowledge. Triage exists to throw away the majority so the minority survives somewhere it will be read.

## The already-recorded test

Before a candidate gets any disposition other than `discard-as-noise`, prove the fact is not already carried by:

- the code itself, including names, types, and structure;
- the tests, which encode intended behavior and its edge cases;
- the commit history and PR description, which encode what changed and why;
- an existing ADR or architecture document;
- the current instruction files;
- the generated project context.

If a competent reader with the repository in front of them would reach the same conclusion, the candidate is noise. "We used the repository's existing test command" is noise. "The repository's test command silently skips the integration suite unless a service is running" is not.

State the test result explicitly for each candidate. An unexamined candidate is not triaged.

## Disposition rules

### `emit-adr`

A decision between real alternatives that constrains future work.

Required: the decision, the alternatives that lost, the evidence, the consequences accepted, and the condition that would justify revisiting it. A record without rejected alternatives is a description, not a decision, and belongs in the work log.

Do not emit an ADR for a choice with no alternative, for applying an existing standard, or for a preference nothing depends on.

### `emit-instruction-rule`

A correction to how agents should work in this repository, either stated by the operator or proven by a failure that would otherwise repeat.

Required: the rule as an imperative, the reason, and the situation that triggers it. A rule whose reason is missing gets obeyed in situations it was never meant for and never gets retired.

Prefer amending an existing rule over adding a similar one. Two rules that partly overlap are worse than one rule that is slightly longer.

Do not emit a rule from a single annoyance. Wait for evidence that it generalizes, or record it as agent memory marked `Inferred` until a second occurrence confirms it.

### `emit-agent-memory`

A durable project fact the repository does not encode and cannot encode: the real behavior of an external system, an environment constraint, a deployment quirk, a decision made outside the repository, or an ongoing goal that shapes work.

Required: the fact, why it is not derivable from the repository, and what would make it stale. Convert relative dates to absolute.

Do not store structure, history, or anything the repository already records. Do not store a summary of the current task; store what outlives it.

### `emit-work-log`

The narrative: what was requested, what was delivered, which gates ran, what was deferred, and what remains open. Useful for tracing and for the next person picking the thread up, not for deciding anything.

Required: dates, exact identifiers such as branch, commits, PR, and run ID, and the honest final state including anything not reached.

### `discard-as-noise`

Everything else, and this is most of it: restatements of the code, steps that went as expected, transient tooling friction, and impressions without evidence.

Discarding is a result. List discarded candidates in the report so the operator can disagree with the filter rather than wonder whether it ran.

## Ranking what survives

When several candidates survive, order them by how expensive the missing knowledge would be next time:

1. something that will silently produce a wrong result;
2. something that will waste significant time rediscovering;
3. something that will cause a wrong decision at a fork;
4. something merely convenient to know.

Emit from the top. If a run yields more than two or three durable emissions, re-run the already-recorded test on the weakest ones; the filter was probably too loose.

## Negative lessons

A workaround that was accepted, a gate that was deferred and never closed, an assumption that turned out wrong, and a target below the selected terminal outcome are the highest-value candidates in a closeout, because nothing else in the repository records them as unfinished.

Record them with their trigger: what would force the workaround to be replaced, and who owns that. A negative lesson without an owner and a trigger becomes permanent by default.
