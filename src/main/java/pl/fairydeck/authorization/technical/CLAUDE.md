# technical

Cross-cutting plumbing that knows nothing about cards or money: `logbook` (correlation id, one log line per
request) and `httpclient` (the shape of every outbound call: retry policy, correlation header). May depend on
Spring and libraries, never on `domain` or `application`. Tested with mock servlet objects and WireMock, without
a Spring context.

<!-- BEGIN project-context-initializer:router -->
## Generated project context

@.agents/project-context.md
<!-- END project-context-initializer:router -->
