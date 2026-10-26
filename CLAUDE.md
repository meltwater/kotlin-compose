# CLAUDE.md

<!-- horace-rules:start -->
@~/.claude/horace/rules/horace-project-conventions.md
@~/.claude/horace/rules/tech-stack.md
@~/.claude/horace/rules/engineering-standards.md
<!-- horace-rules:end -->

## Project Overview

`kotlin-compose` is a Kotlin library maintained by Team Horace that wraps Docker Compose for the JVM, intended primarily for embedded integration tests. It manages the lifecycle of Docker Compose environments (start, stop, inspect) from within JVM test code.

**Tech Stack**: Kotlin 2.3, JDK 21, Gradle (standalone, not Waldo-based), Jackson 2.21, SLF4J, Apache Commons Lang3/IO, Maven Artifact (version comparison)

## Module Structure

This is a single-module project with no Gradle subprojects. All source lives under `src/main/kotlin` and `src/test/kotlin`.

## Architecture

### Key components

- `DockerCompose` (`src/main/kotlin/com/meltwater/docker/compose/DockerCompose.kt`) — primary entry point; manages a docker-compose project (up, kill, rm, ps), copies compose YAML to a temp location, and writes a companion `.env` file from the provided environment map
- `Docker` (`src/main/kotlin/com/meltwater/docker/compose/Docker.kt`) — thin wrapper around the `docker inspect` command for querying container state
- `ExecUtils` (`src/main/kotlin/com/meltwater/docker/compose/ExecUtils.kt`) — executes shell commands synchronously and asynchronously, streaming stdout/stderr to caller-supplied lambdas
- `PullPolicy` / `Recreate` (`src/main/kotlin/com/meltwater/docker/compose/`) — enums controlling `--pull` and `--force-recreate` flags passed to `docker compose up`

## Testing

- JUnit 6 with AssertJ; tests spin up real Docker containers (busybox) via the library itself
- `DockerComposeTest` and `DockerComposeKillTest` are live integration tests — they require Docker to be running locally

## Repo-specific patterns and quirks

- This repo does **not** use the Waldo plugin or `platform-common-dependencies`; it manages its own dependency versions directly in `build.gradle.kts` and publishes to Meltwater Artifactory (`team-horace` repo) rather than via the standard Waldo release flow.
- Supports both the legacy standalone `docker-compose` binary and the modern `docker compose` plugin; detects which is available on `PATH` at runtime.
- Published under group `com.meltwater.docker`, not `com.meltwater.fairhair`.
