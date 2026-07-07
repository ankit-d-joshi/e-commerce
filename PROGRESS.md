# Progress Tracker

Read this file first at the start of any new session to resume cleanly — Claude Code
does not retain memory between sessions.

## Decisions log

- 2026-07-07 — Java version: pinned to **Java 25 LTS** (already installed locally),
  instead of the brief's original Java 21 LTS baseline. User chose this since it's newer
  and nothing extra needed to install.

## Status: Step 1 — First Spring service (Product Catalog)

**Step 0 complete (2026-07-07).** Toolchain installed and verified:

| Tool | Version |
|------|---------|
| Java | OpenJDK 25 (LTS) |
| git | 2.43.0 |
| Maven | 3.8.7 |
| Docker | 29.6.1 (+ Compose v5.3.1) |
| `kind` | 0.32.0 |
| `kubectl` | 1.36.2 |
| `helm` | 4.2.2 |

OS: Linux Mint 22.3 (Ubuntu/Debian-based).

A local `kind` cluster named `ecommerce-dev` is up (`kubectl cluster-info --context
kind-ecommerce-dev`, `kubectl get nodes` both confirmed working). Concepts covered:
`kind` nodes as Docker containers, kubeconfig/contexts and why `kubectl` is always
scoped to a single current-context (verified hands-on: `current-context` switches to a
newly-created cluster automatically, it doesn't aggregate across clusters). Quiz passed.

Git repo initialized at repo root, default branch renamed `master` → `main`, minimal
`.gitignore` added (Editor only — no Java-specific entries yet since no Maven
project exists until Step 1).

**Next action:** first commit (docs + repo scaffolding) — see command below — then begin
Step 1: Product Catalog service (Spring Boot + REST + PostgreSQL).

## Roadmap checklist

- [x] Step 0 — Environment & "see it work first"
- [ ] Step 1 — First Spring service (Product Catalog)
- [ ] Step 2 — Containerize it (Dockerfile + Compose)
- [ ] Step 3 — Onto Kubernetes (hand-written manifests)
- [ ] Step 4 — Make it observable (Actuator, Prometheus, Grafana)
- [ ] Step 5 — Second service + database-per-service (Order)
- [ ] Step 6 — Single entry point (gateway/ingress)
- [ ] Step 7 — Asynchronous messaging (Kafka)
- [ ] Step 8 — Distributed tracing
- [ ] Step 9 — Reliability mechanics
- [ ] Step 10 — Package with Helm
- [ ] Step 11 — CI/CD
- [ ] Step 12 — Security & hardening
- [ ] Step 13 — SLOs & alerting
- [ ] Step 14 — Documentation & graduation artifacts
- [ ] Step 15 — (Optional, paid) Cloud deployment
