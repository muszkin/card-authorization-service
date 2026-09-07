# application

Use cases (`AuthorizePurchase`, `AuthorizationBooking`, `AuthorizationLifecycle`, `IssueCard`, `CardQueries`,
`CardLookup`) and the outbound ports under `port/out`. Transaction boundaries are declared here and nowhere
else; remote calls stay outside them. This layer may use Spring but must not import anything from `adapter`.
Tested with the in-memory port fakes from `src/testFixtures`; the concurrency guard and the outbox atomicity
are additionally proven in `src/integrationTest`.

<!-- BEGIN project-context-initializer:router -->
## Generated project context

@.agents/project-context.md
<!-- END project-context-initializer:router -->
