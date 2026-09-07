# domain

Business rules only: `Money`, `Card`, the append-only ledger and `Balance`, `Authorization` with its transitions
and `AuthorizationPolicy` with the decision rules. Nothing here may import Spring, Jakarta, JDBC or any other
layer; ArchUnit fails the build if it does. Behaviour lives on the types (records and `Authorization`), not in
services. Tested with plain JUnit and AssertJ in milliseconds; a domain change starts with a failing test in
`src/test/java/.../domain`.

<!-- BEGIN project-context-initializer:router -->
## Generated project context

@.agents/project-context.md
<!-- END project-context-initializer:router -->
