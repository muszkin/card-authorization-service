# Spike protocol

A spike buys one decision with a bounded amount of throwaway work. Everything here exists to stop it turning into an unreviewed prototype that ships.

## Before writing any code

Record all of the following. A spike that starts without them cannot be evaluated afterwards.

| Field | Content |
| --- | --- |
| Question | The single falsifiable claim under test |
| Success criteria | The observation that would support `adopt` |
| Kill criteria | The observation that would end the spike immediately |
| Budget | Time box and the point at which the spike is abandoned regardless of progress |
| Environment | Runtime, versions, dataset, and how they relate to production reality |
| Isolation | Worktree path plus every applicable resource from the canonical isolation contract |
| Authorization | Dependencies to install, network calls, external accounts, and any cost |

Kill criteria are the important half. Write the result that would make you stop, and mean it. A spike whose only outcome is "it works" was designed to succeed.

## Isolation

Run in a disposable Git worktree created from the current base commit, never in the user's checkout and never in a worktree another assignment owns. Allocate exclusive runtime resources from the orchestrator's [resource isolation contract](../../implementation-orchestrator/references/resource-isolation.md), which is the same list every concurrent assignment leases from.

Install dependencies only inside that worktree, and only within the authorization recorded above. Never modify the project's lockfile as a side effect of a spike; if the spike needs a dependency added to the real manifest to run at all, that is itself a finding to report, not a change to make.

In a project without version control, or where a worktree cannot be created, use a disposable copy of the project in a temporary directory instead, and record that it is a copy rather than a worktree so nothing is later mistaken for a branch that can be merged.

When safe isolation is unavailable by any of these routes, record `blocked` and report what is missing. Do not fall back to experimenting in the working checkout.

## Running it

Write the narrowest code that makes the question falsifiable. A spike is allowed to be ugly, hardcoded, and untested; it is not allowed to be broad. If the spike grows a second concern, split it or stop.

Change one variable at a time and record what each change ruled out. A spike that changed four things and then worked has proved nothing about which of them mattered.

Use realistic inputs. A result on toy data does not transfer to the project's actual data shape, volume, or concurrency, and claiming otherwise is the most common way a spike misleads a plan.

Capture evidence as it happens: commands with their working directory, versions of everything involved, raw output, measurements with their method, screenshots or traces where the surface supports it. Evidence reconstructed from memory after teardown is not evidence.

## Measurement discipline

When the question is quantitative:

- state the method, the sample size, and the warm-up before quoting a number;
- report the distribution, not one favourable run;
- measure the incumbent under the same conditions, or the comparison is meaningless;
- name the conditions under which the number would not hold.

A single timing from one run of a cold process is an anecdote. Label it as one.

## Stopping

Stop at whichever comes first: success criteria met, kill criteria met, or budget exhausted.

Budget exhaustion is a legitimate result. It reports `needs-more-evidence` with what remains open and what would close it, and that is more useful than an over-budget spike that produces a tired conclusion.

Do not extend the budget to rescue a preferred option. If the budget is extended, record who authorized it and why, and treat the extension as evidence about the option's difficulty.

## Teardown

Tear down regardless of the verdict:

1. Extract the evidence and the artifact into the durable research report first.
2. Stop every process, container, and background job the spike started.
3. Release every resource lease it held, following the release discipline in the isolation contract.
4. Remove the disposable worktree and its branch.
5. Confirm the user's checkout, the lockfiles, and any shared environment are unchanged.

Report the teardown explicitly. A spike worktree left behind will be found later by someone who assumes it was intentional.

## The one-way door

Spike code never becomes the implementation. It was written without tests, without review, and against criteria that measured a question rather than a requirement.

When the verdict is `adopt`, the implementation starts from the plan, not from the spike branch. The spike's contribution is the evidence and the pitfalls it exposed, both of which belong in the research report where the plan can read them.
