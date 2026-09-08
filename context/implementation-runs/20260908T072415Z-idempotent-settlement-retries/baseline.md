# Baseline

- Base SHA: `37e3d118c1116bb2e28ae754a26156f7c44100eb` on `main`; user checkout clean (`git status --porcelain` empty) at 2026-09-08T07:24:15Z.
- `./gradlew build` at this SHA (main checkout, 2026-09-08): BUILD SUCCESSFUL; test 102/0, integrationTest 20/0, acceptanceTest 9/0; compiler with `-Xlint:all,-serial -Werror` clean.
- Known flaky or failing tests: none observed across 8 full runs since 2026-09-07.
- Sonar: not configured (no config in build files or CI) -> NOT_APPLICABLE.
- Staging: none.
- Slice-relevant current behaviour: second capture/reverse -> IllegalStateException -> 409 (see plan "Current state").
