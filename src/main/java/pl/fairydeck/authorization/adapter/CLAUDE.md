# adapter

`in/rest` translates HTTP into use case calls and failures into problem details. `out/*` implements the ports:
`persistence` (JdbcClient + Flyway), `cache` (Redis), `risk` (HTTP client), `events` (outbox and relay). Outbound
adapters are independent slices and must not import each other; the same goes for `in` and `out`. Controllers
are tested in `@WebMvcTest` slices with mocked use cases, outbound adapters against real containers in
`src/integrationTest`, the risk client against WireMock.

<!-- BEGIN project-context-initializer:router -->
## Generated project context

@.agents/project-context.md
<!-- END project-context-initializer:router -->
