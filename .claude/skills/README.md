# Skills

Workflow skills from my own agent toolkit, copied here complete because they reference each other.

| Skill | Use |
| --- | --- |
| `project-context-initializer` | build or refresh the repository map under `context/map` and the scoped `.agents/project-context.md` files |
| `implementation-planning` | turn a non-trivial change into an approval-gated plan with vertical slices and real-surface verification |
| `implementation-orchestrator` | execute an approved plan through isolated workers and SHA-bound quality gates |
| `research-spike` | settle a technology question with primary sources and a disposable spike |
| `systematic-debugging` | reproduce, narrow and root-cause a defect before any fix |
| `incoming-change-review` | review a change this pipeline did not produce, such as somebody else's pull request |
| `task-closeout` | triage what a finished task taught into a decision record, an instruction rule, agent memory or a work log |

Each `SKILL.md` starts with frontmatter and loads its `references/` progressively; some references live in a sibling
bundle, which is why every directory is kept whole.
