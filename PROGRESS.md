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

## Status: Step 2 — Containerize with Docker — COMPLETE, committed

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

**Next action:** ready to start **Step 3 — onto Kubernetes** (hand-written manifests:
Deployment, Service, ConfigMap, Secret, readiness/liveness probes) on explicit user
confirmation. Nothing else pending.

## Roadmap checklist

- [x] Step 0 — Environment & "see it work first"
- [x] Step 1 — First Spring service (Product Catalog)
- [x] Step 2 — Containerize it (Dockerfile + Compose)
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
