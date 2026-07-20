# Progress Tracker

Read this file first at the start of any new session to resume cleanly — Claude Code
does not retain memory between sessions.

## Decisions log

- 2026-07-07 — Java version: pinned to **Java 25 LTS** (already installed locally),
  instead of the brief's original Java 21 LTS baseline. User chose this since it's newer
  and nothing extra needed to install.
- 2026-07-07 (Step 1) — **Spring Boot 4.1.0**, not the originally-pinned 3.x: Spring Boot
  3.5 reached open-source end-of-life 2026-06-30 (last release 3.5.16); 4.1.0 is the
  current GA line (Spring Framework 7) and explicitly supports Java 25.
- 2026-07-07 (Step 1) — **Flyway** chosen for database migrations (SQL-first, most
  popular, best teaching vehicle) over Liquibase.
- 2026-07-07 (Step 1) — **Standalone per-service directory** layout (`product-catalog/`
  with its own self-contained `pom.xml`), not a multi-module Maven parent — each service
  stays independently buildable/deployable, mirroring real microservices. No parent pom
  until/unless a real cross-service need for one appears.
- 2026-07-07 (Step 1) — Two new durable operating rules added to `CLAUDE.md`:
  comprehensive test coverage is required for all application logic, and every future
  decision defaults to the industry-standard/professionally-defensible option over the
  expedient shortcut (see CLAUDE.md operating rules 8–9 for full wording).
- 2026-07-07 (Step 1) — **JaCoCo 0.8.14** pinned for coverage (first release with
  official, non-experimental Java 25 bytecode support).
- 2026-07-07 (Step 1) — **Testcontainers** (real Postgres, pinned `postgres:18.4`) used
  for all tests that touch persistence, instead of H2 — proves the actual Flyway
  migrations run and matches production behavior exactly.
- 2026-07-07 (Step 1) — Toolchain rough edge: `spring-boot-resttestclient:4.1.0`'s
  published POM is missing two of its own runtime dependencies
  (`spring-boot-restclient`, `spring-boot-http-client`) — `TestRestTemplate` throws
  `NoClassDefFoundError` without them. Worked around by declaring both explicitly as
  test-scope dependencies in `product-catalog/pom.xml`. Worth rechecking on the next
  Spring Boot 4.1.x patch release — may be fixed upstream.
- 2026-07-08 (Step 1) — **Teaching contract rewritten**: the "senior Java engineer,
  build freely" split is gone. Java/Spring application code now gets the same
  teach-while-writing + quiz treatment as cloud-native work, and the commenting rule was
  strengthened to cover *every* annotation/API/construct with no "obvious" exception —
  triggered by 8 concrete gaps the user found in Step 1's test files (e.g. `@MockitoBean`
  vs `@Mock`, `@Import`, `@LocalServerPort`, `proxyBeanMethods`). All of Step 1's Java
  files (main + test) were retrofitted with inline comments closing those gaps before
  first commit; `CLAUDE.md`'s "What this project is" and "teaching contract" sections
  were rewritten to match. See `CLAUDE.md` for the current wording.
- 2026-07-10 (Step 2) — **`eclipse-temurin:25-jdk-noble` / `25-jre-noble`** pinned as the
  Dockerfile's build/runtime base images (Debian-based; no Alpine variant exists for Java
  25). Runtime stage runs as a dedicated non-root user — cheap baseline hardening, not the
  deep hardening (distroless, read-only FS, scanning, RBAC) explicitly scoped to Step 12.
- 2026-07-10 (Step 2) — **BuildKit cache mount** (`--mount=type=cache,target=/root/.m2`)
  added to both Maven `RUN` instructions after the first build attempt failed 54 minutes
  in on a Maven Central read timeout. Root cause: a normal Docker layer only commits on
  success, so a failed `RUN` discards every dependency already downloaded and a retry
  starts from zero; a cache mount persists outside the layer system and survives a failed
  `RUN`, so retries resume instead of re-downloading everything. Requires the
  `# syntax=docker/dockerfile:1` parser directive as the Dockerfile's first line.
- 2026-07-10 (Step 2) — **`<finalName>app</finalName>`** added to `product-catalog/pom.xml`
  so the built jar is always `target/app.jar` regardless of `<version>`, decoupling the
  Dockerfile from the project's version string.
- 2026-07-10 (Step 2) — Toolchain rough edge: an initial Dockerfile draft used
  `ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]` (the
  pre-3.2-era layered-jar pattern) and failed with `ClassNotFoundException`. Verified by
  inspecting the actual extracted output that Spring Boot 4.1.0's `-Djarmode=tools
  extract --layers` now rewrites the jar into a **thin jar** whose manifest sets
  `Main-Class` to the app's own main class and `Class-Path` to the `dependencies/`
  layer's `lib/*.jar` files directly — no loader class involved, and the
  `spring-boot-loader/` extracted directory is empty for this packaging model (kept only
  for forward-compat with Spring Boot's documented 4-layer contract). Fixed to
  `ENTRYPOINT ["java", "-jar", "app.jar"]`. Worth rechecking on future Spring Boot
  versions in case the loader-class layout returns.
- 2026-07-10 (Step 2) — **Datasource config externalized** via
  `${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/product_catalog}`-style
  placeholders in `application.yml`, so the same jar works unmodified both bare
  (`./mvnw spring-boot:run`, localhost fallback) and under Docker Compose (env vars
  injected, pointing at the `postgres` service name over Compose's internal DNS).
- 2026-07-19 (Step 3) — **Minimal Actuator added now**, rather than deferring all of
  Actuator to Step 4: only the `health` endpoint is exposed over HTTP, with the
  Kubernetes `liveness`/`readiness` probe groups enabled (`management.endpoint.health
  .probes.enabled: true`). The `db` health indicator is placed in the **readiness**
  group only, deliberately excluded from **liveness** — a database outage should drop a
  pod from its Service's endpoints (readiness), not get the pod killed and restarted
  (liveness), since restarting the JVM does nothing to fix a database problem. Full
  Actuator breadth (metrics, Prometheus scraping) stays deferred to Step 4 per YAGNI.
- 2026-07-19 (Step 3) — **Postgres runs as a Deployment + PVC** in-cluster, not a
  StatefulSet: simpler manifest surface for a single-instance dev database, at the cost
  of the StatefulSet teaching moment (deferred until/unless a real multi-instance
  stateful need arises). `product-catalog` itself is a Deployment with `replicas: 2`
  (stateless, so trivially horizontal — the intended contrast with Postgres's
  `replicas: 1`).
- 2026-07-19 (Step 3) — **`k8s/` manifest filenames prefixed `kubernetes-`** (e.g.
  `kubernetes-13-postgres-deployment.yaml`), at the user's request: their YAML editor
  extension only enables Kubernetes schema checking/autocomplete for filenames
  containing "kubernetes". A numeric segment follows the prefix to encode
  `kubectl apply -f k8s/`'s alphabetical apply order (namespace → config/secret/storage
  → postgres → product-catalog).
- 2026-07-19 (Step 3) — Toolchain/behavior note (not a bug, left as-is): Kubernetes has
  no equivalent of Compose's `depends_on: condition: service_healthy`. On first apply,
  `postgres` and `product-catalog` Deployments start racing simultaneously, so
  `product-catalog`'s pods hit "Connection to postgres:5432 refused" during Flyway's
  startup connection attempt (fatal to `ApplicationContext` refresh, unlike a
  *post-startup* DB blip, which the readiness/liveness split is designed to survive
  without a restart) and are restarted by Kubernetes' own backoff loop until Postgres
  becomes reachable. No manual intervention needed; self-heals within roughly a minute
  on this local cluster.

## Status: Step 3 — Onto Kubernetes — COMPLETE, committed

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

**Step 1 build complete (2026-07-07), quiz passed, committed.** Built `product-catalog/`:
a standalone Spring Boot 4.1.0 service with `GET /api/products` (paginated, via
`PagedModel`) and `GET /api/products/{id}`, backed by PostgreSQL. Domain: `Product`
entity + `ProductRepository`; schema owned entirely by two Flyway migrations
(`V1__create_products_table.sql`, `V2__insert_seed_products.sql`), Hibernate set to
`ddl-auto: validate` so it never touches the schema itself. Errors surface as RFC 9457
`ProblemDetail` via a `@RestControllerAdvice`.

Four-tier test suite (12 tests, 0 failures): Mockito unit test on `ProductService`;
`@WebMvcTest` slice on `ProductController`; `@DataJpaTest` + Testcontainers
(`postgres:18.4`) proving the Flyway migrations run against a real database; full
`@SpringBootTest(RANDOM_PORT)` end-to-end test over real HTTP. JaCoCo 0.8.14 reports
**100% instruction coverage on every application class** (the only gap is
`ProductCatalogApplication.main()`, which only executes when the app is actually
launched). `mvn clean verify` passes end-to-end.

Flyway + Testcontainers quiz passed (2026-07-08) — user correctly explained
`ddl-auto: validate` as the ORM/migration-tool responsibility split, why a shipped
migration is edited via a new version rather than in place, what
`flyway_schema_history` checksums protect against, why Testcontainers-over-H2 avoids
the works-locally-not-in-prod dialect trap, and what `@ServiceConnection` auto-wires.

Committed 2026-07-08 (19 commits, `01794ab`..`f9e6851` — scaffold + the teaching-contract
rewrite + full comment retrofit, split granularly by the user rather than the two
originally-proposed commits). Step 1 is fully done.

**Step 2 build complete (2026-07-10), quiz passed, committed.** Built
`product-catalog/Dockerfile`: a heavily-commented multi-stage build (`eclipse-temurin:
25-jdk-noble` build stage → `25-jre-noble` runtime stage, non-root user, layered-jar
extraction via `-Djarmode=tools extract --layers`, BuildKit cache-mounted Maven builds).
Added `product-catalog/.dockerignore` and root-level `docker-compose.yml` (Postgres +
product-catalog services, named `pgdata` volume, `pg_isready` healthcheck gating
`depends_on: condition: service_healthy`, Compose-internal DNS via service name).
`application.yml`'s datasource block externalized to `${SPRING_DATASOURCE_*:localhost
fallback}` placeholders so the same jar runs bare or under Compose unmodified.
`product-catalog/pom.xml` pinned `<finalName>app</finalName>` to decouple the Dockerfile
from the project version.

Two real bugs were hit and fixed during build verification, both logged above in the
decisions log: a 54-minute Maven Central timeout (fixed via BuildKit `--mount=type=cache`
for `/root/.m2`) and a `ClassNotFoundException` from an incorrect `JarLauncher`
`ENTRYPOINT` copied from pre-3.2-era tutorials (fixed to `java -jar app.jar` after
verifying Spring Boot 4.1.0's actual thin-jar-plus-`Class-Path` extraction output).

End-to-end verified: `docker compose up --build` builds cleanly, Postgres reports
healthy, Flyway migrates on container startup, `GET /api/products` and
`GET /api/products/{id}` both return correct data (and a correct 404 `ProblemDetail` for
a missing id) against the running container, `docker compose down -v` cleanly resets
state, and `mvn clean verify` on the host still passes all 12 tests unaffected by the
containerization changes. Final runtime image: 531MB (Debian-based JRE, not
slim/distroless — that trade-off is deliberate per Step 2's scope; deeper hardening is
Step 12).

Quiz passed (2026-07-10) — user correctly explained Compose service-name DNS vs
`localhost` inside a container, and the `Main-Class`/`Class-Path` manifest fields that let
`java -jar app.jar` resolve the app and its dependencies with no loader class. Two answers
needed a follow-up clarification (both confirmed understood after): why a *failed* `RUN`
normally discards all progress but a BuildKit cache mount survives that failure boundary;
and what specifically in Postgres's own entrypoint script (a two-phase startup — a
temporary `initdb`/init-script instance that shuts down, then the real long-lived server)
makes "container started" different from "ready for connections."

Committed 2026-07-10 (7 commits, `0112953`..`223cca6`). Step 2 is fully done.

**Step 3 build complete (2026-07-19), quiz passed, committed.** Two decisions confirmed with the user
up front (also logged in the decisions log below): a *minimal* Actuator (health endpoint +
Kubernetes `liveness`/`readiness` probe groups only, metrics/Prometheus deferred to Step 4)
rather than deferring probes entirely; and Postgres as a Deployment+PVC in-cluster rather
than a StatefulSet, since the app itself is already a stateless Deployment.

Application changes: `product-catalog/pom.xml` gained `spring-boot-starter-actuator`;
`application.yml` gained a commented `management:` block exposing only `health`, with the
`db` indicator placed in the **readiness** group but explicitly excluded from **liveness**
— a database outage should drop the pod from the Service's endpoints, not get the pod
killed and restarted. `mvn clean verify` re-run and confirmed all 12 tests still pass with
Actuator added.

New `k8s/` directory: 9 manifests, filenames prefixed `kubernetes-` (per user request, so
their YAML extension's Kubernetes schema checking applies) and numbered for a safe
`kubectl apply -f k8s/` order — Namespace → shared `db-credentials` Secret + `db-config`/
`catalog-config` ConfigMaps → `postgres-pvc` (1Gi, `standard` StorageClass, dynamically
provisioned) → `postgres` Deployment (`replicas: 1`, `strategy: Recreate` — required
because a ReadWriteOnce PVC can't be mounted read-write by two pods at once, which
RollingUpdate would briefly attempt — + `pg_isready` exec probes, mirroring the Compose
healthcheck) → `postgres` Service (ClusterIP, name must be exactly `postgres` for JDBC URL
DNS resolution to keep working unmodified) → `product-catalog` Deployment (`replicas: 2`,
default RollingUpdate since it's stateless, `imagePullPolicy: IfNotPresent` since there's no
registry — `kind load docker-image` puts the image directly on the node — plus a
`startupProbe` gating `livenessProbe`/`readinessProbe`, the former pointed at
`/actuator/health/liveness`, the latter at `/actuator/health/readiness`) → `product-catalog`
Service (ClusterIP, port 8080).

End-to-end verified against the running `ecommerce-dev` kind cluster: `kubectl apply
--dry-run=client` validated all 9 manifests; image built and `kind load`ed; all pods reached
`1/1 Running`; Flyway migrated both migrations on the successful catalog boot;
`GET /api/products`, `/api/products/1`, and a correct 404 `ProblemDetail` for
`/api/products/9999` all verified via `kubectl port-forward`; both `/actuator/health/{live
ness,readiness}` returned `{"status":"UP"}`. Resilience checks: deleting a `product-catalog`
pod kept the Service serving via the surviving replica and the ReplicaSet's replacement pod
came up clean; deleting the `postgres` pod recreated it and the product data (verified by an
identical `createdAt` timestamp) survived via the PVC, proving persistence.

One real, instructive behavior was hit and is intentionally left as a teaching artifact
rather than "fixed": Kubernetes has no equivalent of Compose's
`depends_on: condition: service_healthy` — the `postgres` and `product-catalog` Deployments
both started the instant they were applied, so on this first-ever apply the catalog pods hit
"Connection to postgres:5432 refused" during Flyway's startup connection attempt (Flyway
failing during `ApplicationContext` refresh is fatal to the app, unlike a database blip
*after* the app is already up, which the liveness/readiness split above is designed to
survive). Kubernetes' own restart-with-backoff loop recovered automatically once Postgres
became reachable, with zero manual intervention — worth calling out explicitly in the quiz,
since it's a real ordering gap this step's design doesn't paper over.

`README.md` gained a "Run on Kubernetes (kind)" section (build, `kind load`,
`kubectl apply -f k8s/`, port-forward, teardown) documenting this same flow.

Quiz passed (2026-07-19) — user correctly explained: why `postgres`'s Deployment needs
`strategy: Recreate` (a ReadWriteOnce PVC can only be mounted read-write by one Pod at a
time; `RollingUpdate` would briefly try to run old and new Pods side by side and deadlock)
and named the resulting cost (momentary unavailability during a Postgres update) as an
acceptable trade; what concretely goes wrong if `db` were moved into the `liveness` group
instead of `readiness` (the app Pod would be restarted on a DB blip, which does nothing to
fix a database problem — wasted restart churn); the Deployment → ReplicaSet → Pod
ownership chain and what each layer is individually responsible for; and why the
first-apply "Connection to postgres:5432 refused" restarts happened and why nothing needed
fixing (no Kubernetes equivalent of Compose's `depends_on: condition: service_healthy`;
Kubernetes' own restart-with-backoff loop self-heals once Postgres becomes reachable).

Committed 2026-07-19 (14 commits, `61305d2`..`32503a0`). Step 3 is fully done.

**Next action:** ready to start **Step 4 — make it observable** (structured logging, the
rest of Spring Boot Actuator, Prometheus + Grafana scraping the service with one
dashboard) on explicit user confirmation. Nothing else pending.

## Roadmap checklist

- [x] Step 0 — Environment & "see it work first"
- [x] Step 1 — First Spring service (Product Catalog)
- [x] Step 2 — Containerize it (Dockerfile + Compose)
- [x] Step 3 — Onto Kubernetes (hand-written manifests)
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
