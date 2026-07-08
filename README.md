# Spring Microservices E-Commerce (Learning Build)

A hands-on e-commerce application built to gain **genuine, interview-defensible
competence** in Spring Boot microservices, containerization, and Kubernetes/cloud —
starting from a decade of Spring/Java experience but new to the cloud-native stack.

See [CLAUDE.md](CLAUDE.md) for the working agreement/teaching contract that governs how
this project is built, and [PROGRESS.md](PROGRESS.md) for current status.

## Goals

1. Refresh Spring Boot knowledge
2. Learn microservices architecture
3. Learn Java features introduced since Java 8+
4. Learn cloud technologies (AWS)
5. Learn containerization (Docker)
6. Learn orchestration (Kubernetes)
7. Build interview-defensible depth for Senior/Principal Software Engineer roles

## Stack (pinned)

| Component      | Choice                          |
|-----------------|---------------------------------|
| Language        | Java 25 (LTS)                   |
| Framework       | Spring Boot 4.1.0 (updated from the original 3.x pin — 3.5 reached open-source EOL 2026-06-30) |
| Build tool      | Maven                           |
| Database        | PostgreSQL (one per service)    |
| Migrations      | Flyway                          |
| Containers      | Docker / Docker Compose         |
| Orchestration   | Kubernetes via `kind` (local)   |
| Packaging       | Helm (introduced in Step 10)    |
| Messaging       | Kafka (introduced in Step 7)    |
| Observability   | Prometheus, Grafana, OpenTelemetry + Jaeger/Tempo |
| Resilience      | Resilience4j                    |
| CI/CD           | GitHub Actions                  |

Everything runs locally and free until the optional, explicitly-confirmed final cloud
step.

## Roadmap

### Phase 0 — Foundations
- **Step 0** — Environment & "see it work first": install/verify toolchain (JDK, build
  tool, Docker, `kind`, `kubectl`, `helm`), stand up an empty `kind` cluster and confirm
  `kubectl` talks to it. Claude guides installation, the user runs it — Claude does not
  install tools directly.

### Phase 1 — One service, all the way through
- **Step 1** — First Spring service: Product Catalog (list/get products), plain Spring
  Boot + REST, backed by PostgreSQL. App logic — Claude's to write freely.
- **Step 2** — Containerize it: a well-commented multi-stage Dockerfile (build stage +
  slim runtime, layered jar) teaching Docker fundamentals. Run the service + a Postgres
  container locally via Docker Compose and confirm it works.
- **Step 3** — Onto Kubernetes: hand-write the manifests (Deployment, Service, ConfigMap,
  Secret, readiness/liveness probes) and deploy to `kind`. Core learning moment — teach
  and quiz thoroughly. Hand-write before we ever templatize (see Step 10).
- **Step 4** — Make it observable: structured logging + Spring Boot Actuator; get
  Prometheus + Grafana into the cluster scraping the service, with one dashboard. Skeleton
  complete: one service, containerized, on k8s, observable.

### Phase 2 — Grow into real microservices
- **Step 5** — Second service (Order) with its own database (database-per-service); a
  synchronous Order→Catalog call. Teach how a service finds another via k8s DNS / Service
  names, contrasted with Spring Cloud Eureka — where the platform replaces the framework.
- **Step 6** — Single entry point: a gateway/ingress. Explain the tradeoff between Spring
  Cloud Gateway (as a service) and a Kubernetes Ingress, **then ask the user which one to
  implement** before building it.
- **Step 7** — Asynchronous messaging via Kafka: placing an order publishes an
  `OrderPlaced` event consumed by an Inventory service. Teach async decoupling,
  at-least-once delivery, and idempotency — what makes this genuinely microservices.

### Phase 3 — The production-grade envelope
- **Step 8** — Distributed tracing: OpenTelemetry → Jaeger/Tempo, tracing one request
  across gateway → order → catalog → broker → consumer.
- **Step 9** — Reliability mechanics: Resilience4j (circuit breakers, retries with
  backoff, timeouts) on inter-service calls; Horizontal Pod Autoscaling; graceful
  shutdown; a dead-letter queue + replay for the async path. Introduce each as the answer
  to a specific failure mode.
- **Step 10** — Package with Helm: convert the hand-written manifests into a Helm chart.
  Teach templating, values, and environment overrides — now that the raw YAML Helm is
  abstracting is already understood.
- **Step 11** — CI/CD via GitHub Actions: build, test, scan the image, push to a
  registry, deploy. GitOps with Argo CD is an optional stretch — **only if the user asks
  for it.**

### Phase 4 — Operate, harden, document, graduate
- **Step 12** — Security & hardening: secrets management, least-privilege RBAC, network
  policies, TLS, non-root containers, image scanning.
- **Step 13** — SLOs & alerting: define one real SLO (e.g. 99% of orders processed within
  N seconds), wire Alertmanager to alert on breach, run a synthetic load generator and
  operate against the SLO.
- **Step 14** — Make it legible to a hiring manager: README with architecture diagram, an
  ADR log (architecture decision records explaining the *why* behind key choices),
  runbooks, and one written incident post-mortem.
- **Step 15** — *(Optional, the only paid step)* Cloud deployment to a managed cluster
  (EKS/GKE/AKS) with strict cost discipline and teardown. Defer until everything above is
  solid; confirm with the user before incurring any cost.

## Status

In progress — see [PROGRESS.md](PROGRESS.md).
