# Change intake

Everything after this depends on reviewing the right bytes. Get the revision and the scope wrong and the rest of the review is confident nonsense.

## Identify the source

| Source | What to establish |
| --- | --- |
| Pull or merge request | Number, target branch, head revision, whether the branch is behind the target, and whether a merge queue will rebase it |
| Branch | Its merge base with the intended target, and whether it exists only locally |
| Patch or archive | The revision it was generated against and whether that revision is reachable |
| Vendored or dependency update | The upstream revision range, the upstream changelog, and whether the diff matches the claimed range |

Record the head revision explicitly. Every finding is bound to it, and any push makes the review stale rather than merely outdated.

## Compute the right diff

Use the merge base, not the target's current tip:

```text
git fetch <remote> <target> <head>
git merge-base <target> <head>
git diff <merge-base>...<head>
```

The three-dot form against the merge base shows the author's work. A two-dot diff against a moved target mixes in other people's commits and produces findings the author cannot act on.

For a rebased or force-pushed branch, note that earlier review comments may be attached to revisions that no longer exist, and that a prior approval no longer applies.

### Without version control or a reachable base

A patch file, an archive, a vendor drop or a non-Git project gives no revision to pin. Substitute a content identity: record a SHA-256 of the exact reviewed bytes, plus whatever the sender states about the base, marked as a claim rather than a verified fact.

Say explicitly that the base could not be verified. Without it, "this change does not touch X" is unprovable, and every finding about preserved behavior is `Inferred` rather than `Observed`.

## Separate the diff into kinds

Before reading, classify every changed path:

- **behavior** — source that changes what the product does;
- **tests** — read later, as specifications;
- **generated** — output of a tool, verified by regenerating rather than by reading;
- **vendored** — third-party code, verified against the upstream range;
- **configuration and infrastructure** — often the highest blast radius per line;
- **documentation** — read for contradictions with the behavior change;
- **noise** — formatting, renames, and moves.

Reviewing noise line by line while skimming configuration is the most common way a review misses the defect. If noise and behavior are mixed in the same files, ask for the split; if the split is refused, review the behavior lines specifically and record that the noise was not individually inspected.

## Reconstruct the intent from evidence

Sources, in decreasing reliability:

1. the linked issue or requirement, when it states observable behavior;
2. the tests added or changed, which encode the intended contract;
3. the public interfaces the change introduces or alters;
4. the ADRs and architecture documents covering the area;
5. the change description, which states an intent that may or may not match the diff;
6. commit messages.

A gap between the stated intent and the diff is a finding, in both directions: undelivered intent, and undocumented behavior nobody asked for.

## Establish the working surface

Review from a separate clean checkout or worktree pinned to the head revision, never from a mutable working copy that could contaminate what you read.

Determine what you may run: read-only commands, the test suite, static analysis, a local build. Running the project's own tests against the change is the strongest verification available to a reviewer and should be used when the environment allows it.

Record what you could not run and why. A dimension you could not assess is reported as `not-applicable` with its reason, never silently passed.

## Establish preserved behavior

Before judging the change, know what must not break: existing consumers of the touched contracts, stored data written by the previous version, mixed-version runtime during rollout, and behavior other tests depend on.

The author knows what they intended to change. The reviewer's distinct value is knowing what they did not intend to change and did.
