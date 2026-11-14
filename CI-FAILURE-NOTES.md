# CI workflow refinement

The previous GitHub Actions annotations contained runtime deprecation warnings and a separate Gradle job failure. The warnings alone do not identify the Gradle failure cause.

Changes to `.github/workflows/build.yml`:
- Upgrade checkout to v6, setup-java to v5, setup-gradle to v6, and upload-artifact to v5 (Node.js 24-compatible action generations).
- Pin the runner to Ubuntu 24.04 rather than following an unplanned `ubuntu-latest` image migration.
- Explicitly install Android API 34 and Build Tools 34.0.0, which match `app/build.gradle` (`compileSdk 34`).
- Run Gradle with `--stacktrace` and `--warning-mode all` so the next failure exposes its actual cause.
- Run on pull requests as well as pushes to `main` and manual dispatches.
- Fail clearly if the APK artifact is missing.

Important: this workflow change removes the known deprecated action generations and makes SDK setup explicit. It does not prove the original Gradle failure is fixed because the supplied annotations do not include the failing Gradle output. Inspect the `Build debug APK` log after the next run if it still fails.
