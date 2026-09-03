# Contributing to Courier

This document describes how to build and validate changes to Courier.

## Requirements

- JDK 17 through 26
- Git

## Build and test

From the repository root:

```bash
cd burp-extension
./gradlew clean check
```

The `check` task compiles the Java 17 source, runs the test suite, and validates
the platform-specific JAR.

To generate the release JAR, CycloneDX SBOM, and SHA-256 checksum:

```bash
./gradlew clean check releaseArtifacts
```

Release outputs are written to `burp-extension/build/releases/`.

## Proposing changes

Propose changes through a GitHub pull request against `main`.

See [README.md](README.md) for architecture, setup, and workflow documentation.
Participation in this project is covered by the
[Code of Conduct](CODE_OF_CONDUCT.md). Courier is distributed under the
[Apache License 2.0](LICENSE).
