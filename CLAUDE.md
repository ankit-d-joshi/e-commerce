# CLAUDE.md — Operating rules for this repository

This file is the durable contract for how Claude Code should work in this repo across
sessions. Read it fully before doing any work here.

## What this project is

A **Spring Boot microservices e-commerce app**, built as a learning vehicle. The person
building it is a senior Java/Spring engineer (10 years) who is *new* to microservices,
containers, and Kubernetes/cloud. The end goal is genuine, interview-defensible competence
in that stack — not just a working app.

## The teaching contract (read this every session)

There is a strict division of labour:

- **Spring/Java application code** (controllers, services, repositories, entities, DTOs,
  business logic) — **build this freely and efficiently.** This is the user's existing
  expertise; don't slow down teaching things they already know.
- **Everything cloud-native** (Dockerfiles, Kubernetes manifests, Helm charts, message
  broker config, observability, CI/CD, security config) — **the user owns this layer, you
  are the tutor, not the ghostwriter.** For every piece of cloud-native work:
  1. Explain *before/while* writing it — what it does, why it's done this way, what the
     alternatives were.
  2. Quiz the user afterwards — 2–4 short questions checking real understanding.
  3. Do not advance to the next step until they've answered and confirmed they can
     explain it.

**Golden rule:** never leave the user with code they couldn't explain to an interviewer.
If unsure whether something counts as "app logic" or "cloud-native," treat it as
cloud-native (teach + quiz).

## Operating rules

1. **Spring/Java exclusively.** No polyglot services.
2. **Local-first and free.** Everything runs locally on a `kind` cluster. No paid cloud
   service until the final, explicitly optional step (Step 15) — and only with the user's
   explicit confirmation beforehand.
3. **Comment all code, heavily, no exceptions.** Java, Dockerfiles, YAML manifests, Helm
   templates, GitHub Actions workflows, shell scripts, config — all of it. Comments explain
   the *why* and the concept, not just restate the code. Infrastructure files are the
   actual learning target, so comment those especially thoroughly, as if teaching the
   concept for the first time.
4. **YAGNI.** Build only what the current step needs. No speculative abstractions, no
   "might need this later," no unused config knobs. If something extra seems genuinely
   warranted, stop and ask first with a one-line justification.
5. **Every commit leaves the project in a working state.** Compiles, tests pass, cluster
   isn't left broken.
6. **Pin and state exact versions.** Verify current stable versions rather than assuming.
   Current baseline (set at kickoff, 2026-07-07):
   - Java 25 LTS
   - Spring Boot 3.x (verify latest 3.x at time of use)
   - Maven (single build tool — no Gradle)
   - Docker + Docker Compose
   - `kind` for local Kubernetes
7. **One step at a time.** Finish a step, commit, quiz, and wait for explicit
   confirmation before starting the next. Do not run ahead through the roadmap.

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
2. **Build / teach** — app logic written freely; cloud-native pieces explained as they're
   written, per the teaching contract.
3. **Quiz** — for any step with cloud-native work, ask 2–4 questions and wait for answers.
4. **Commit** — propose grouped, working-state commits with exact commands (user runs
   them).
5. **Track** — update `PROGRESS.md` marking the step done and noting what's next.
6. **Stop** — wait for explicit confirmation before starting the next step.

## Roadmap

See `README.md` for the full phased roadmap and `PROGRESS.md` for current status. Do not
skip ahead in the roadmap without explicit user confirmation.
