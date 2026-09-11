# Runtime leases

Diagnosis, S1 and S2 workers/reviewers/E2E/integration: RELEASED after completed Gradle processes.
Final verification: pending. No concurrent runtime gates or CI/local test overlap.
Harness uses dynamic application/WireMock ports and fresh Testcontainers; no shared services or Compose.
Post-S2 Docker query for Testcontainers-owned containers returned no containers.
