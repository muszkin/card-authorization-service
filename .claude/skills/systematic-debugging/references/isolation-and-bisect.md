# Isolation and bisection

Narrowing turns a reproduction into a minimal case. The discipline is the same on every axis: change one thing, predict the result, record what it ruled out.

## The ruled-out ledger

Maintain it from the first experiment. Without it, an investigation revisits disproved theories and cannot be handed to anyone else.

| # | Variable changed | Prediction | Result | Ruled out |
| --- | --- | --- | --- | --- |

An experiment with no prediction recorded before it ran is not evidence; hindsight will fit any result to any theory.

## Bisecting history

Use when the defect is a regression and a known-good revision exists.

1. Confirm the failure at the suspected bad revision and its absence at the known-good one, using the same reproduction and the same controlled state.
2. Automate the check into a script with a clean exit code so the search is mechanical rather than a series of judgement calls.
3. Run `git bisect` between them.
4. Account for revisions that cannot be built or tested. Skip them explicitly rather than guessing their verdict.
5. Verify the identified commit by reverting it in isolation, or by applying only its change to the good revision.

The commit that introduces a failure is often not the commit that contains the defect: it may be the one that started exercising a latent bug. Say which of the two you found.

### When there is no usable history

A shallow clone, a squashed import, a non-Git project or a defect reported against a binary release all remove the history axis. Do not treat that as the end of narrowing; substitute the coarsest ordered sequence that exists — released versions, tagged builds, deployment records, artifact digests, or dated backups — and binary-search that instead. Fetch missing history first when the repository is merely shallow.

When no ordered sequence exists at all, record the history axis as unavailable with its reason and narrow on the input, configuration, dependency and state axes only. An investigation that cannot bisect history is slower, not blocked.

## Bisecting inputs

Use when one input fails and similar inputs do not.

Halve the input and retest, keeping it valid at each step. Continue until removing anything makes the failure disappear. Then describe the minimal input in terms of its properties, not its literal bytes: the length, the boundary value, the encoding, the null, the duplicate, the ordering.

The property is what the regression test asserts. The literal input is a sample of it.

## Bisecting configuration and environment

Use when it fails in one environment and not another.

Enumerate every difference before changing any: versions actually resolved, configuration and flags, data, identity and permissions, platform, locale and timezone, network path, and resource limits. Then move one difference at a time from the working environment toward the failing one, or the reverse, and record which single change flips the outcome.

Resist the temptation to change several at once because the loop is slow. A slow loop with one variable is faster than a fast loop that proves nothing.

## Bisecting dependencies

Use when a dependency upgrade is suspected.

Compare the actually resolved tree, not the declared ranges: a transitive dependency moved by a lockfile refresh is a common cause and is invisible in the manifest diff. Pin one dependency back at a time, and read the upstream changelog and open issues for the exact version boundary you find.

## Isolating shared state

Use when a test passes alone and fails in a suite, or the reverse.

Suspect order dependence, leaked global or module state, a shared database or cache not reset between cases, a background job outliving its test, filesystem or temporary-path collisions, a frozen or drifting clock, and parallel workers sharing a resource.

Reproduce by running the suite in the failing order with everything else removed, then bisect the case list. The other case that must be present is the finding.

## Concurrency and timing

Use when the failure is intermittent and load-dependent.

Increase the pressure deliberately rather than waiting for luck: raise concurrency, shrink timeouts, slow a dependency, or insert a delay at a suspected interleaving point. Making the failure more frequent is itself a hypothesis test, because whatever raises the rate is close to the cause.

Never make it disappear by adding a sleep. That converts a reproducible defect into an intermittent one.

## Instrumentation

When narrowing stalls, add temporary observation rather than temporary fixes.

Log at the boundaries the value crosses, not only where the error surfaces. Trace the actual data through the path and compare it against the assumption at each hop. The place where the observed value first diverges from the expected one is the cause's neighbourhood.

Keep instrumentation in the isolated worktree and remove it before handoff, unless the investigation concluded the code was genuinely unobservable there, which is a finding worth keeping.

## Knowing when to stop narrowing

Stop when the case is minimal: removing any remaining element makes the failure disappear.

If narrowing has produced no reduction after several experiments, the failure is probably not on the axis being searched. Return to the reproduction, widen the boundary, and re-examine what the report said rather than what the investigation assumed it said.
