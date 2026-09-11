# Topology

Base/main: d7537cdb2b25fbf6a6b4ec7bed4dbddd8bf274fb. Feature: fix/safe-numeric-boundaries.
Only build workflow; PR/main pushes run ./gradlew build on [self-hosted, home].
Branch protection query:404 unprotected; build is still mandatory under the plan.
Prior CI run34331600544 on base succeeded; runner availability rechecked during delivery.
No configured Sonar, dependency/license/secret scanner, staging or production deployment.
Static gate: compiler -Xlint:all,-serial -Werror plus ArchUnit; inspect introduced dependencies/secrets in diff.
Local integration fast-forward, remote merge commit preserving history.
