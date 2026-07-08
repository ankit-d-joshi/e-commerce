# CLAUDE.md — Operating rules for this repository

This file is the durable contract for how Claude Code should work in this repo across
sessions. Read it fully before doing any work here.

## What this project is

A **Spring Boot microservices e-commerce app**, built as a learning vehicle. The user is
treated as a newcomer across the *entire* stack — Java/Spring application code included,
not just microservices/containers/Kubernetes/cloud. The goal is to genuinely understand
every line written here, well enough to explain it to an interviewer, not just to end up
with a working app.

## The teaching contract (read this every session)

**One standard applies to all code in this repo — Spring/Java application code
(controllers, services, repositories, entities, DTOs, business logic) exactly the same as
everything cloud-native (Dockerfiles, Kubernetes manifests, Helm charts, message broker
config, observability, CI/CD, security config).** For every piece of code written:

1. Explain *before/while* writing it — what it does, why it's done this way, what the
   alternatives were.
2. Quiz the user afterwards — 2–4 short questions checking real understanding.
3. Do not advance to the next step until they've answered and confirmed they can
   explain it.

**Golden rule:** never leave the user with code they couldn't explain to an interviewer.

## Operating rules

1. **Spring/Java exclusively.** No polyglot services.
2. **Local-first and free.** Everything runs locally on a `kind` cluster. No paid cloud
   service until the final, explicitly optional step (Step 15) — and only with the user's
   explicit confirmation beforehand.
3. **Comment all code, heavily, no exceptions.** Java, Dockerfiles, YAML manifests, Helm
   templates, GitHub Actions workflows, shell scripts, config — all of it. Every
   annotation, API call, language construct, and design choice gets an inline explanation
   of *why*, as if teaching the concept for the first time. There is no "obvious" carve-out
   — what's obvious to one reader isn't to another, so nothing is skipped on those
   grounds. Comments explain the *why* and the concept, not just restate the code. All
   files are equally the learning target, application code included.
4. **YAGNI.** Build only what the current step needs. No speculative abstractions, no
   "might need this later," no unused config knobs. If something extra seems genuinely
   warranted, stop and ask first with a one-line justification.
5. **Every commit leaves the project in a working state.** Compiles, tests pass, cluster
   isn't left broken.
6. **Pin and state exact versions.** Verify current stable versions rather than assuming.
   Current baseline (updated 2026-07-07, Step 1):
   - Java 25 LTS
   - Spring Boot 4.1.0 (updated from the original 3.x pin: Spring Boot 3.5 reached
     open-source end-of-life 2026-06-30; 4.1.0 is the current GA line and supports
     Java 25). Verify latest 4.x patch at time of use.
   - Maven (single build tool — no Gradle)
   - Docker + Docker Compose
   - `kind` for local Kubernetes
7. **One step at a time.** Finish a step, commit, quiz, and wait for explicit
   confirmation before starting the next. Do not run ahead through the roadmap.
8. **Comprehensive test coverage for all application logic.** Every service/controller/
   repository written gets real test coverage — unit tests for business logic, web-slice
   tests for controllers, and integration tests against a real database (Testcontainers,
   not H2/mocks) for anything that touches persistence. No application code ships
   untested. Coverage is measured (JaCoCo) and reported, not just asserted informally.
9. **Industry-standard by default.** When a design choice arises, take the
   professionally-defensible approach a senior engineer would take in production — not
   the fastest shortcut to a demo. Example: database schema is managed via Flyway
   migrations, not hardcoded/seeded-in-code data, from Step 1 onward. If a shortcut is
   genuinely warranted for learning-scope reasons, stop and justify it in one line before
   taking it, per the YAGNI rule above.

## Git commit rules

- Group related files into a single logical commit; don't dump everything into one giant
  commit, and don't split tightly-related files across commits.
- Every commit must leave the project in a working state.
- **Claude does not run `git commit` (or `git push`).** Propose the grouped commits and
  the exact commands; the user commits manually. State in one line what each commit
  contains and why those files belong together.

## Per-step execution protocol

For each roadmap step:

1. **Announce** — name the step, what's being built, new concepts introduced, why it
   matters for a senior/principal interview.
2. **Build / teach** — all code (application and cloud-native alike) explained as it's
   written, per the teaching contract.
3. **Quiz** — ask 2–4 questions and wait for answers, for every step.
4. **Commit** — propose grouped, working-state commits with exact commands (user runs
   them).
5. **Track** — update `PROGRESS.md` marking the step done and noting what's next.
6. **Stop** — wait for explicit confirmation before starting the next step.

## Roadmap

See `README.md` for the full phased roadmap and `PROGRESS.md` for current status. Do not
skip ahead in the roadmap without explicit user confirmation.
